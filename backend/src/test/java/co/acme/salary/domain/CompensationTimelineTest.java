package co.acme.salary.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.acme.salary.domain.CompensationTimeline.InvalidCompensationChange;
import co.acme.salary.testsupport.Fixtures;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The timeline is the heart of the product, so it is tested as plain Java with a fixed clock and a
 * fixed rate table — no Spring, no database, nothing that can make a run non-repeatable.
 */
class CompensationTimelineTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final FxRateTable RATES = FxRateTable.CURRENT;

    private final Employee employee = Fixtures.germanEmployee();

    private CompensationTimeline emptyTimeline() {
        return CompensationTimeline.of(employee, List.of());
    }

    private CompensationTimeline hiredTimeline() {
        CompensationTimeline timeline = emptyTimeline();
        timeline.recordChange(Money.of(80_000, "EUR"), Fixtures.HIRE_DATE, ChangeReason.HIRE,
                "Offer accepted", RATES, NOW);
        return timeline;
    }

    @Nested
    @DisplayName("the first record")
    class FirstRecord {

        @Test
        void opensTheTimelineAndStaysOpen() {
            CompensationTimeline timeline = hiredTimeline();

            assertThat(timeline.current()).isPresent();
            assertThat(timeline.current().orElseThrow().isOpen()).isTrue();
            assertThat(timeline.current().orElseThrow().salary()).isEqualTo(Money.of(80_000, "EUR"));
        }

        @Test
        void mustBeRecordedAsAHire() {
            assertThatThrownBy(() -> emptyTimeline().recordChange(
                    Money.of(80_000, "EUR"), Fixtures.HIRE_DATE, ChangeReason.MERIT_INCREASE, null, RATES, NOW))
                    .isInstanceOf(InvalidCompensationChange.class)
                    .hasMessageContaining("HIRE");
        }

        @Test
        void isConvertedIntoTheReportingCurrencyAtWriteTime() {
            CompensationRecord hire = hiredTimeline().current().orElseThrow();

            // 80,000 EUR at the shipped rate of 1.0850
            assertThat(hire.annualUsd()).isEqualTo(Money.of(86_800, "USD"));
            assertThat(hire.getFxRateVersion()).isEqualTo(RATES.version());
        }
    }

    @Nested
    @DisplayName("a pay change")
    class PayChange {

        private final LocalDate promotionDate = LocalDate.of(2022, 7, 1);

        @Test
        void closesThePreviousRecordTheDayTheNewOneStarts() {
            CompensationTimeline timeline = hiredTimeline();
            CompensationRecord hire = timeline.current().orElseThrow();

            timeline.recordChange(Money.of(95_000, "EUR"), promotionDate, ChangeReason.PROMOTION,
                    "Promoted to Staff", RATES, NOW);

            assertThat(hire.isOpen()).isFalse();
            assertThat(hire.getEffectiveTo()).isEqualTo(promotionDate);
        }

        @Test
        void leavesExactlyOneOpenRecord() {
            CompensationTimeline timeline = hiredTimeline();

            timeline.recordChange(Money.of(95_000, "EUR"), promotionDate, ChangeReason.PROMOTION, null, RATES, NOW);
            timeline.recordChange(Money.of(99_000, "EUR"), LocalDate.of(2024, 1, 1),
                    ChangeReason.MERIT_INCREASE, null, RATES, NOW);

            assertThat(timeline.records()).filteredOn(CompensationRecord::isOpen).hasSize(1);
            assertThat(timeline.current().orElseThrow().salary()).isEqualTo(Money.of(99_000, "EUR"));
        }

        @Test
        void leavesNoGapBetweenPeriods() {
            CompensationTimeline timeline = hiredTimeline();
            timeline.recordChange(Money.of(95_000, "EUR"), promotionDate, ChangeReason.PROMOTION, null, RATES, NOW);

            List<CompensationRecord> history = timeline.records();
            assertThat(history.get(0).getEffectiveTo()).isEqualTo(history.get(1).getEffectiveFrom());
        }

        @Test
        void cannotRewriteHistoryByBackdatingBeforeTheCurrentRecord() {
            CompensationTimeline timeline = hiredTimeline();
            timeline.recordChange(Money.of(95_000, "EUR"), promotionDate, ChangeReason.PROMOTION, null, RATES, NOW);

            assertThatThrownBy(() -> timeline.recordChange(Money.of(99_000, "EUR"),
                    LocalDate.of(2021, 1, 1), ChangeReason.MERIT_INCREASE, null, RATES, NOW))
                    .isInstanceOf(InvalidCompensationChange.class)
                    .hasMessageContaining("superseded");
        }

        @Test
        void cannotTakeEffectOnTheSameDayAsTheCurrentRecord() {
            CompensationTimeline timeline = hiredTimeline();

            assertThatThrownBy(() -> timeline.recordChange(Money.of(90_000, "EUR"),
                    Fixtures.HIRE_DATE, ChangeReason.MERIT_INCREASE, null, RATES, NOW))
                    .isInstanceOf(InvalidCompensationChange.class);
        }

        @Test
        void cannotIntroduceASecondHireRecord() {
            CompensationTimeline timeline = hiredTimeline();

            assertThatThrownBy(() -> timeline.recordChange(Money.of(90_000, "EUR"),
                    promotionDate, ChangeReason.HIRE, null, RATES, NOW))
                    .isInstanceOf(InvalidCompensationChange.class)
                    .hasMessageContaining("already has a hire salary");
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        void rejectsANonPositiveSalary() {
            assertThatThrownBy(() -> emptyTimeline().recordChange(Money.of(0, "EUR"),
                    Fixtures.HIRE_DATE, ChangeReason.HIRE, null, RATES, NOW))
                    .isInstanceOf(InvalidCompensationChange.class)
                    .hasMessageContaining("greater than zero");
        }

        @Test
        void rejectsACurrencyTheEmployeeIsNotPaidIn() {
            assertThatThrownBy(() -> emptyTimeline().recordChange(Money.usd(90_000),
                    Fixtures.HIRE_DATE, ChangeReason.HIRE, null, RATES, NOW))
                    .isInstanceOf(InvalidCompensationChange.class)
                    .hasMessageContaining("paid in EUR");
        }

        @Test
        void rejectsAnEffectiveDateBeforeTheHireDate() {
            assertThatThrownBy(() -> emptyTimeline().recordChange(Money.of(80_000, "EUR"),
                    Fixtures.HIRE_DATE.minusDays(1), ChangeReason.HIRE, null, RATES, NOW))
                    .isInstanceOf(InvalidCompensationChange.class)
                    .hasMessageContaining("before the hire date");
        }
    }

    @Nested
    @DisplayName("answers what someone earned on a past date")
    class HistoricalLookup {

        private CompensationTimeline promotedTimeline() {
            CompensationTimeline timeline = hiredTimeline();
            timeline.recordChange(Money.of(95_000, "EUR"), LocalDate.of(2022, 7, 1),
                    ChangeReason.PROMOTION, null, RATES, NOW);
            return timeline;
        }

        @Test
        void returnsTheSalaryInForceOnThatDay() {
            assertThat(promotedTimeline().asOf(LocalDate.of(2021, 6, 1)).orElseThrow().salary())
                    .isEqualTo(Money.of(80_000, "EUR"));
        }

        @Test
        void treatsTheEffectiveDateItselfAsTheNewSalary() {
            assertThat(promotedTimeline().asOf(LocalDate.of(2022, 7, 1)).orElseThrow().salary())
                    .isEqualTo(Money.of(95_000, "EUR"));
        }

        @Test
        void treatsTheDayBeforeAsTheOldSalary() {
            assertThat(promotedTimeline().asOf(LocalDate.of(2022, 6, 30)).orElseThrow().salary())
                    .isEqualTo(Money.of(80_000, "EUR"));
        }

        @Test
        void hasNothingToSayAboutDatesBeforeTheHire() {
            assertThat(promotedTimeline().asOf(LocalDate.of(2019, 1, 1))).isEmpty();
        }
    }
}
