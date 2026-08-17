package co.acme.salary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One period during which an employee was paid a particular annual base salary.
 *
 * <p>Records form a gap-free, non-overlapping timeline per employee. The interval is closed-open:
 * {@code effectiveFrom} inclusive, {@code effectiveTo} exclusive, with {@code null} meaning "still
 * in force". Records are never edited in place; a pay change closes the open record and appends a
 * new one, which is what makes the history auditable.
 *
 * <p>{@code annualUsdAmount} and {@code fxRateVersion} are denormalised at write time so that
 * cross-country aggregation is a SQL {@code SUM} rather than ten thousand conversions per request
 * (ADR-004). Storing the rate version keeps that derived value explainable after the fact.
 */
@Entity
@Table(name = "compensation_record", indexes = {
        @Index(name = "idx_comp_employee_open", columnList = "employee_id, effective_to"),
        @Index(name = "idx_comp_effective_from", columnList = "effective_from")
})
public class CompensationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** Annual base salary in the employee's payroll currency. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "annual_usd_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal annualUsdAmount;

    @Column(name = "fx_rate_version", nullable = false, length = 16)
    private String fxRateVersion;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** Null while this is the salary currently in force. */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_reason", nullable = false, length = 32)
    private ChangeReason changeReason;

    @Column(length = 500)
    private String note;

    /** When the change was entered, as opposed to when it takes effect. */
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected CompensationRecord() {
        // for JPA
    }

    public CompensationRecord(Employee employee, Money salary, Money annualUsd, String fxRateVersion,
                              LocalDate effectiveFrom, ChangeReason changeReason, String note,
                              Instant recordedAt) {
        this.employee = employee;
        this.amount = salary.amount();
        this.currencyCode = salary.currencyCode();
        this.annualUsdAmount = annualUsd.amount();
        this.fxRateVersion = fxRateVersion;
        this.effectiveFrom = effectiveFrom;
        this.changeReason = changeReason;
        this.note = note;
        this.recordedAt = recordedAt;
    }

    public Money salary() {
        return Money.of(amount, currencyCode);
    }

    public Money annualUsd() {
        return Money.of(annualUsdAmount, "USD");
    }

    public boolean isOpen() {
        return effectiveTo == null;
    }

    /**
     * Closes this record the day the next one starts. Package-private on purpose: only
     * {@code CompensationTimeline} may close a record, so the invariant has exactly one owner.
     */
    void closeOn(LocalDate date) {
        if (!isOpen()) {
            throw new IllegalStateException("Compensation record " + id + " is already closed");
        }
        if (!date.isAfter(effectiveFrom)) {
            throw new IllegalArgumentException(
                    "Cannot close a record on %s: it starts on %s".formatted(date, effectiveFrom));
        }
        this.effectiveTo = date;
    }

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public BigDecimal getAnnualUsdAmount() {
        return annualUsdAmount;
    }

    public String getFxRateVersion() {
        return fxRateVersion;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public ChangeReason getChangeReason() {
        return changeReason;
    }

    public String getNote() {
        return note;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
