package co.acme.salary.query;

import co.acme.salary.domain.ChangeReason;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One entry in an employee's salary history, as the UI shows it.
 *
 * <p>{@code changePercent} is the raise relative to the preceding period — null for the hire
 * record, since there is nothing to compare it to. It is derived here rather than in the browser
 * so that the number in the timeline and the number in a report can never disagree.
 */
public record CompensationEntry(
        Long id,
        BigDecimal amount,
        String currencyCode,
        BigDecimal annualUsdAmount,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        ChangeReason reason,
        String reasonLabel,
        String note,
        Instant recordedAt,
        BigDecimal changePercent,
        boolean current) {
}
