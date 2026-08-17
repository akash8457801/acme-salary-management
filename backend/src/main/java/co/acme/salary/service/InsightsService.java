package co.acme.salary.service;

import co.acme.salary.domain.Gender;
import co.acme.salary.domain.JobLevel;
import co.acme.salary.query.BreakdownDimension;
import co.acme.salary.query.InsightModels.BreakdownRow;
import co.acme.salary.query.InsightModels.InsightsDashboard;
import co.acme.salary.query.InsightModels.PayEquityRow;
import co.acme.salary.repository.InsightsRepository;
import co.acme.salary.repository.InsightsRepository.GenderMedian;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Answers the "how do we pay people" half of the product. */
@Service
public class InsightsService {

    private final InsightsRepository insights;

    public InsightsService(InsightsRepository insights) {
        this.insights = insights;
    }

    @Transactional(readOnly = true)
    public InsightsDashboard dashboard() {
        return new InsightsDashboard(
                insights.overview(),
                insights.breakdownBy(BreakdownDimension.DEPARTMENT),
                insights.breakdownBy(BreakdownDimension.COUNTRY),
                byLevelInLevelOrder(),
                insights.distribution(),
                payEquity());
    }

    @Transactional(readOnly = true)
    public List<BreakdownRow> breakdown(BreakdownDimension dimension) {
        return dimension == BreakdownDimension.LEVEL
                ? byLevelInLevelOrder()
                : insights.breakdownBy(dimension);
    }

    /**
     * Levels are a ladder, so they read as one — L1 to L7 — rather than being ordered by cost the
     * way departments and countries are.
     */
    private List<BreakdownRow> byLevelInLevelOrder() {
        return insights.breakdownBy(BreakdownDimension.LEVEL).stream()
                .sorted((left, right) -> JobLevel.valueOf(left.key()).compareTo(JobLevel.valueOf(right.key())))
                .map(row -> new BreakdownRow(
                        row.key(),
                        row.key() + " · " + JobLevel.valueOf(row.key()).title(),
                        row.headcount(),
                        row.totalAnnualUsd(),
                        row.averageAnnualUsd(),
                        row.medianAnnualUsd(),
                        row.p25AnnualUsd(),
                        row.p75AnnualUsd()))
                .toList();
    }

    /**
     * Median pay by gender, compared within a level.
     *
     * <p>Comparing across the whole org would mostly measure who holds senior roles, which is a
     * real problem but a different one. Comparing like with like is what makes the number
     * actionable: it says whether two people doing the same job are paid the same.
     */
    @Transactional(readOnly = true)
    public List<PayEquityRow> payEquity() {
        Map<String, List<GenderMedian>> byLevel = insights.medianByLevelAndGender().stream()
                .collect(Collectors.groupingBy(GenderMedian::level));

        List<PayEquityRow> rows = new ArrayList<>();
        for (JobLevel level : JobLevel.values()) {
            List<GenderMedian> medians = byLevel.get(level.name());
            if (medians == null || medians.isEmpty()) {
                continue;
            }
            Map<String, GenderMedian> byGender = medians.stream()
                    .collect(Collectors.toMap(GenderMedian::gender, Function.identity()));

            GenderMedian female = byGender.get(Gender.FEMALE.name());
            GenderMedian male = byGender.get(Gender.MALE.name());
            long other = medians.stream()
                    .filter(median -> !median.gender().equals(Gender.FEMALE.name()))
                    .filter(median -> !median.gender().equals(Gender.MALE.name()))
                    .mapToLong(GenderMedian::headcount)
                    .sum();

            rows.add(new PayEquityRow(
                    level.name(),
                    headcountOf(female),
                    medianOf(female),
                    headcountOf(male),
                    medianOf(male),
                    gapPercent(medianOf(female), medianOf(male)),
                    other));
        }
        return rows;
    }

    /**
     * How far the female median sits below the male median, as a percentage of the male median.
     * Positive means women are paid less. Null when either group is missing — a gap computed from
     * one person is noise, and reporting it as zero would be worse than reporting nothing.
     */
    private BigDecimal gapPercent(BigDecimal femaleMedian, BigDecimal maleMedian) {
        if (femaleMedian == null || maleMedian == null || maleMedian.signum() == 0) {
            return null;
        }
        return maleMedian.subtract(femaleMedian)
                .multiply(BigDecimal.valueOf(100))
                .divide(maleMedian, 1, RoundingMode.HALF_UP);
    }

    private long headcountOf(GenderMedian median) {
        return median == null ? 0 : median.headcount();
    }

    private BigDecimal medianOf(GenderMedian median) {
        return median == null ? null : median.medianUsd();
    }
}
