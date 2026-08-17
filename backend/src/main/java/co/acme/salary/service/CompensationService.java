package co.acme.salary.service;

import co.acme.salary.domain.ChangeReason;
import co.acme.salary.domain.CompensationTimeline;
import co.acme.salary.domain.Employee;
import co.acme.salary.domain.FxRateTable;
import co.acme.salary.domain.Money;
import co.acme.salary.query.CompensationEntry;
import co.acme.salary.repository.CompensationRecordRepository;
import co.acme.salary.repository.EmployeeRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recording what someone is paid, and reading back what they have been paid.
 *
 * <p>The rules themselves live in {@link CompensationTimeline}; this class exists to load the
 * timeline, hand it the change, and persist the result. Closing the previous record and opening
 * the new one happen in a single transaction — a half-applied pay change would leave the timeline
 * either overlapping or gapped, and neither is recoverable from.
 */
@Service
public class CompensationService {

    private final EmployeeRepository employees;
    private final CompensationRecordRepository records;
    private final FxRateTable fxRates;
    private final Clock clock;

    public CompensationService(EmployeeRepository employees, CompensationRecordRepository records,
                               FxRateTable fxRates, Clock clock) {
        this.employees = employees;
        this.records = records;
        this.fxRates = fxRates;
        this.clock = clock;
    }

    /** @return the newly opened period, including how big a change it was */
    @Transactional
    public CompensationEntry recordChange(Long employeeId, Money salary, LocalDate effectiveFrom,
                                          ChangeReason reason, String note) {
        Employee employee = employees.findById(employeeId)
                .orElseThrow(() -> ResourceNotFoundException.employee(employeeId));

        CompensationTimeline timeline = CompensationTimeline.of(
                employee, records.findByEmployeeIdOrderByEffectiveFromAsc(employeeId));

        timeline.recordChange(salary, effectiveFrom, reason, note, fxRates, clock.instant());

        // The predecessor was closed in memory by the timeline; saving the whole list keeps the
        // two halves of the change atomic rather than relying on dirty-checking order.
        records.saveAll(timeline.records());

        List<CompensationEntry> entries = CompensationEntryAssembler.assemble(timeline.records());
        return entries.get(entries.size() - 1);
    }

    @Transactional(readOnly = true)
    public List<CompensationEntry> history(Long employeeId) {
        if (!employees.existsById(employeeId)) {
            throw ResourceNotFoundException.employee(employeeId);
        }
        return CompensationEntryAssembler.assemble(
                records.findByEmployeeIdOrderByEffectiveFromAsc(employeeId));
    }
}
