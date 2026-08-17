package co.acme.salary.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * An employee's compensation history, and the only place allowed to change it.
 *
 * <p>This is the core domain rule of the system, kept deliberately free of Spring and JPA so it
 * can be tested as plain Java. The invariants it guarantees:
 *
 * <ul>
 *   <li>the first record on a timeline is the hire salary;</li>
 *   <li>at most one record is open at a time;</li>
 *   <li>periods never overlap and never leave a gap — closing one record and opening the next
 *       happen together or not at all;</li>
 *   <li>a new record always starts strictly after the current one, so history is superseded,
 *       never rewritten;</li>
 *   <li>salary is always positive and always in the employee's payroll currency.</li>
 * </ul>
 */
public final class CompensationTimeline {

    private final Employee employee;
    private final List<CompensationRecord> records;

    private CompensationTimeline(Employee employee, List<CompensationRecord> records) {
        this.employee = employee;
        this.records = records;
    }

    public static CompensationTimeline of(Employee employee, Collection<CompensationRecord> records) {
        List<CompensationRecord> sorted = new ArrayList<>(records);
        sorted.sort(Comparator.comparing(CompensationRecord::getEffectiveFrom));
        return new CompensationTimeline(employee, sorted);
    }

    /** History, oldest first. */
    public List<CompensationRecord> records() {
        return List.copyOf(records);
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

    /** The salary in force today, if the employee has one. */
    public Optional<CompensationRecord> current() {
        return records.stream().filter(CompensationRecord::isOpen).findFirst();
    }

    /** The salary in force on a given date — the question a spreadsheet cannot answer. */
    public Optional<CompensationRecord> asOf(LocalDate date) {
        return records.stream()
                .filter(record -> !date.isBefore(record.getEffectiveFrom()))
                .filter(record -> record.getEffectiveTo() == null || date.isBefore(record.getEffectiveTo()))
                .findFirst();
    }

    /**
     * Supersedes the current salary with a new one.
     *
     * <p>Closes the open record on {@code effectiveFrom} and appends a new open record, so the
     * timeline stays gap-free. Returns the appended record; the caller is responsible for
     * persisting both it and the now-closed predecessor in one transaction.
     *
     * @throws InvalidCompensationChange if the change would break a timeline invariant
     */
    public CompensationRecord recordChange(Money salary, LocalDate effectiveFrom, ChangeReason reason,
                                           String note, FxRateTable fxRates, Instant recordedAt) {
        validateSalary(salary, fxRates);
        validateEffectiveDate(effectiveFrom, reason);

        current().ifPresent(open -> open.closeOn(effectiveFrom));

        CompensationRecord appended = new CompensationRecord(
                employee,
                salary,
                fxRates.toUsd(salary),
                fxRates.version(),
                effectiveFrom,
                reason,
                note,
                recordedAt);
        records.add(appended);
        return appended;
    }

    private void validateSalary(Money salary, FxRateTable fxRates) {
        if (!salary.isPositive()) {
            throw new InvalidCompensationChange("Salary must be greater than zero");
        }
        if (!salary.currencyCode().equals(employee.payrollCurrency())) {
            throw new InvalidCompensationChange(
                    "%s is paid in %s, so the salary cannot be recorded in %s"
                            .formatted(employee.fullName(), employee.payrollCurrency(), salary.currencyCode()));
        }
        if (!fxRates.supports(salary.currencyCode())) {
            throw new InvalidCompensationChange(
                    "No exchange rate is available for " + salary.currencyCode());
        }
    }

    private void validateEffectiveDate(LocalDate effectiveFrom, ChangeReason reason) {
        if (effectiveFrom.isBefore(employee.getHireDate())) {
            throw new InvalidCompensationChange(
                    "A salary cannot take effect on %s, before the hire date of %s"
                            .formatted(effectiveFrom, employee.getHireDate()));
        }
        if (isEmpty()) {
            if (reason != ChangeReason.HIRE) {
                throw new InvalidCompensationChange(
                        "The first salary on a timeline must be recorded as " + ChangeReason.HIRE);
            }
            return;
        }
        if (reason == ChangeReason.HIRE) {
            throw new InvalidCompensationChange(
                    "%s already has a hire salary recorded".formatted(employee.fullName()));
        }
        CompensationRecord open = current().orElseThrow(() -> new InvalidCompensationChange(
                "%s has no salary currently in force".formatted(employee.fullName())));
        if (!effectiveFrom.isAfter(open.getEffectiveFrom())) {
            throw new InvalidCompensationChange(
                    "A change must take effect after the current salary started on %s. "
                            .formatted(open.getEffectiveFrom())
                            + "Earlier history is superseded, never rewritten.");
        }
    }

    /** Thrown when a requested pay change would break the timeline. Maps to HTTP 422. */
    public static class InvalidCompensationChange extends RuntimeException {
        public InvalidCompensationChange(String message) {
            super(message);
        }
    }
}
