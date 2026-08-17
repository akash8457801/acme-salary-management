package co.acme.salary.query;

import java.math.BigDecimal;
import java.util.List;

/**
 * Read models for the compensation insights screens. All amounts are annualised USD, because a
 * figure that spans ten countries has to be in one currency to mean anything.
 */
public final class InsightModels {

    private InsightModels() {
    }

    /** The headline numbers: what ACME spends on payroll and how it is spread. */
    public record OrgOverview(
            long headcount,
            BigDecimal totalAnnualUsd,
            BigDecimal averageAnnualUsd,
            BigDecimal medianAnnualUsd,
            int countryCount,
            int departmentCount) {
    }

    /** One group — a department, a country or a level — with its cost and salary spread. */
    public record BreakdownRow(
            String key,
            String label,
            long headcount,
            BigDecimal totalAnnualUsd,
            BigDecimal averageAnnualUsd,
            BigDecimal medianAnnualUsd,
            BigDecimal p25AnnualUsd,
            BigDecimal p75AnnualUsd) {
    }

    /** One bar of the salary histogram; {@code upperBoundUsd} is null for the open-ended top band. */
    public record SalaryBand(BigDecimal lowerBoundUsd, BigDecimal upperBoundUsd, long headcount) {
    }

    /** Median pay by gender within one level, and the gap between the two largest groups. */
    public record PayEquityRow(
            String level,
            long femaleCount,
            BigDecimal femaleMedianUsd,
            long maleCount,
            BigDecimal maleMedianUsd,
            BigDecimal gapPercent,
            long otherCount) {
    }

    /** Everything the insights dashboard needs, in one response. */
    public record InsightsDashboard(
            OrgOverview overview,
            List<BreakdownRow> byDepartment,
            List<BreakdownRow> byCountry,
            List<BreakdownRow> byLevel,
            List<SalaryBand> distribution,
            List<PayEquityRow> payEquity) {
    }
}
