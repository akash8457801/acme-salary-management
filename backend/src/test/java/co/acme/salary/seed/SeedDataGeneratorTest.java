package co.acme.salary.seed;

import static org.assertj.core.api.Assertions.assertThat;

import co.acme.salary.domain.ChangeReason;
import co.acme.salary.domain.FxRateTable;
import co.acme.salary.domain.JobLevel;
import co.acme.salary.seed.SeedData.SeededCompensation;
import co.acme.salary.seed.SeedData.SeededEmployee;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Properties of the generated organisation. These are checked over a full 10,000-person run —
 * the same size as production — because distribution bugs (an empty level, a duplicate email at
 * scale) only show up in volume. Generation is pure in-memory work, so this is still fast.
 */
class SeedDataGeneratorTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 8, 1);
    private static List<SeededEmployee> org;

    @BeforeAll
    static void generateOnce() {
        org = new SeedDataGenerator(FxRateTable.CURRENT).generate(10_000, AS_OF);
    }

    @Test
    void producesExactlyTheRequestedHeadcount() {
        assertThat(org).hasSize(10_000);
    }

    @Test
    void isDeterministic_theSameSeedProducesTheSameOrg() {
        List<SeededEmployee> once = new SeedDataGenerator(FxRateTable.CURRENT).generate(100, AS_OF);
        List<SeededEmployee> twice = new SeedDataGenerator(FxRateTable.CURRENT).generate(100, AS_OF);
        assertThat(once).isEqualTo(twice);
    }

    @Test
    void emailsAreUniqueAcrossAllTenThousandPeople() {
        assertThat(org.stream().map(SeededEmployee::email).distinct()).hasSize(10_000);
    }

    @Test
    void everyTimelineStartsWithAHireOnTheHireDate() {
        assertThat(org).allSatisfy(employee -> {
            SeededCompensation first = employee.compensation().get(0);
            assertThat(first.reason()).isEqualTo(ChangeReason.HIRE);
            assertThat(first.effectiveFrom()).isEqualTo(employee.hireDate());
        });
    }

    @Test
    void compensationChangesAreStrictlyChronological() {
        assertThat(org).allSatisfy(employee -> {
            List<SeededCompensation> history = employee.compensation();
            for (int i = 1; i < history.size(); i++) {
                assertThat(history.get(i).effectiveFrom())
                        .isAfter(history.get(i - 1).effectiveFrom());
            }
        });
    }

    @Test
    void everyLevelOfThePyramidIsPopulated_andJuniorsOutnumberExecutives() {
        Map<JobLevel, Long> byLevel = org.stream()
                .collect(Collectors.groupingBy(SeededEmployee::level, Collectors.counting()));

        assertThat(byLevel.keySet()).containsExactlyInAnyOrder(JobLevel.values());
        assertThat(byLevel.get(JobLevel.L2)).isGreaterThan(byLevel.get(JobLevel.L6));
        assertThat(byLevel.get(JobLevel.L7)).isLessThan(500);
    }

    @Test
    void managersAreAlwaysMoreSeniorAndInTheSameDepartment() {
        Map<Long, SeededEmployee> byId = org.stream()
                .collect(Collectors.toMap(SeededEmployee::id, e -> e));

        assertThat(org).allSatisfy(employee -> {
            if (employee.managerId() == null) {
                return;
            }
            SeededEmployee manager = byId.get(employee.managerId());
            assertThat(manager.level().isSeniorTo(employee.level())).isTrue();
            assertThat(manager.departmentName()).isEqualTo(employee.departmentName());
        });
    }

    @Test
    void salariesAreInTheCountrysPayrollCurrency() {
        Map<String, String> currencyByCountry = SeedReferenceData.COUNTRIES.stream()
                .collect(Collectors.toMap(c -> c.code(), c -> c.currency()));

        assertThat(org).allSatisfy(employee ->
                assertThat(employee.currentCompensation().currencyCode())
                        .isEqualTo(currencyByCountry.get(employee.countryCode())));
    }
}
