package co.acme.salary.seed;

import co.acme.salary.domain.ChangeReason;
import co.acme.salary.domain.EmploymentStatus;
import co.acme.salary.domain.FxRateTable;
import co.acme.salary.domain.Gender;
import co.acme.salary.domain.JobLevel;
import co.acme.salary.domain.Money;
import co.acme.salary.seed.SeedData.SeededCompensation;
import co.acme.salary.seed.SeedData.SeededEmployee;
import co.acme.salary.seed.SeedReferenceData.CountrySeed;
import co.acme.salary.seed.SeedReferenceData.DepartmentSeed;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Builds a believable ten-thousand-person organisation.
 *
 * <p>Deterministic by construction: one fixed seed, no clock, no ambient randomness. The same
 * inputs produce byte-identical output every run, so the demo, the screenshots and the tests all
 * describe the same company (ADR-005).
 *
 * <p>The distributions are shaped rather than uniform. Salary follows level, adjusted for the cost
 * of labour in each country and a premium or discount per department, with per-person variance on
 * top. A small number of people are deliberately paid well outside their band, and a modest
 * gender gap is baked in — both so that the insights screens surface something real instead of
 * flat noise. That gap is synthetic test data, not a claim about any real organisation.
 */
public class SeedDataGenerator {

    /** Fixed so that every run of the seeder produces exactly the same organisation. */
    public static final long RANDOM_SEED = 20260101L;

    private static final double GENDER_GAP_FACTOR = 0.965;
    private static final double OUTLIER_PROBABILITY = 0.012;
    private static final double OUTLIER_FACTOR = 1.55;

    private final FxRateTable fxRates;

    public SeedDataGenerator(FxRateTable fxRates) {
        this.fxRates = fxRates;
    }

    /**
     * @param employeeCount how many people to generate
     * @param asOf          "today" for the generated org — hire dates and raises are relative to it
     */
    public List<SeededEmployee> generate(int employeeCount, LocalDate asOf) {
        Random random = new Random(RANDOM_SEED);
        Set<String> usedEmails = new HashSet<>();
        List<SeededEmployee> employees = new ArrayList<>(employeeCount);

        for (int index = 0; index < employeeCount; index++) {
            employees.add(generateEmployee(index + 1L, random, usedEmails, asOf));
        }
        return assignManagers(employees, random);
    }

    private SeededEmployee generateEmployee(long id, Random random, Set<String> usedEmails, LocalDate asOf) {
        DepartmentSeed department = weightedPick(SeedReferenceData.DEPARTMENTS,
                SeedReferenceData.DEPARTMENTS.stream().map(DepartmentSeed::headcountWeight).toList(), random);
        CountrySeed country = weightedPick(SeedReferenceData.COUNTRIES, SeedReferenceData.COUNTRY_WEIGHTS, random);
        JobLevel level = pickLevel(random);
        Gender gender = pickGender(random);

        String firstName = pick(SeedReferenceData.FIRST_NAMES, random);
        String lastName = pick(SeedReferenceData.LAST_NAMES, random);
        String email = uniqueEmail(firstName, lastName, usedEmails);

        LocalDate hireDate = pickHireDate(random, asOf);
        EmploymentStatus status = pickStatus(random);
        String jobTitle = titleFor(department, level, random);

        BigDecimal currentUsd = targetSalaryUsd(level, country, department, gender, random);
        List<SeededCompensation> compensation =
                buildCompensationHistory(currentUsd, country, hireDate, asOf, level, random);

        return new SeededEmployee(id, "ACME-%05d".formatted(id), firstName, lastName, email,
                department.name(), country.code(), jobTitle, level, gender, null, hireDate, status,
                compensation);
    }

    /**
     * Works backwards from what someone earns today: the current salary is the believable number,
     * and each earlier record is derived by undoing a raise. Building forwards from a starting
     * salary would let compounding drift produce £400k support engineers.
     */
    private List<SeededCompensation> buildCompensationHistory(BigDecimal currentUsd, CountrySeed country,
                                                              LocalDate hireDate, LocalDate asOf,
                                                              JobLevel level, Random random) {
        List<LocalDate> changeDates = changeDates(hireDate, asOf, random);

        // Undo each raise in turn to recover the salary at every earlier point in the timeline.
        List<BigDecimal> amountsUsd = new ArrayList<>();
        List<Double> raises = new ArrayList<>();
        BigDecimal amount = currentUsd;
        amountsUsd.add(amount);
        for (int i = 0; i < changeDates.size() - 1; i++) {
            double raise = 0.04 + random.nextDouble() * 0.09;
            raises.add(raise);
            amount = amount.divide(BigDecimal.valueOf(1 + raise), 2, RoundingMode.HALF_UP);
            amountsUsd.add(0, amount);
        }

        List<SeededCompensation> history = new ArrayList<>(changeDates.size());
        for (int i = 0; i < changeDates.size(); i++) {
            ChangeReason reason = i == 0 ? ChangeReason.HIRE : reasonFor(raises.get(i - 1), random);
            Money local = toLocalCurrency(amountsUsd.get(i), country);

            history.add(new SeededCompensation(
                    local.amount(),
                    local.currencyCode(),
                    changeDates.get(i),
                    reason,
                    noteFor(reason, level)));
        }
        return history;
    }

    /** Hire date, then a raise roughly every 12–20 months, capped at three changes. */
    private List<LocalDate> changeDates(LocalDate hireDate, LocalDate asOf, Random random) {
        List<LocalDate> dates = new ArrayList<>();
        dates.add(hireDate);

        LocalDate next = hireDate.plusMonths(12 + random.nextInt(9));
        while (next.isBefore(asOf) && dates.size() < 4) {
            dates.add(next);
            next = next.plusMonths(12 + random.nextInt(9));
        }
        return dates;
    }

    private ChangeReason reasonFor(double raise, Random random) {
        if (raise > 0.10) {
            return ChangeReason.PROMOTION;
        }
        if (random.nextDouble() < 0.18) {
            return ChangeReason.MARKET_ADJUSTMENT;
        }
        return ChangeReason.MERIT_INCREASE;
    }

    private String noteFor(ChangeReason reason, JobLevel level) {
        return switch (reason) {
            case HIRE -> "Starting salary at " + level.name();
            case PROMOTION -> "Promoted following performance review";
            case MERIT_INCREASE -> "Annual merit increase";
            case MARKET_ADJUSTMENT -> "Adjusted to market benchmark";
            case ROLE_CHANGE, CORRECTION -> null;
        };
    }

    private BigDecimal targetSalaryUsd(JobLevel level, CountrySeed country, DepartmentSeed department,
                                       Gender gender, Random random) {
        double base = SeedReferenceData.LEVEL_MIDPOINT_USD.get(level);
        double variance = 0.86 + random.nextDouble() * 0.30;
        double genderFactor = gender == Gender.FEMALE
                ? GENDER_GAP_FACTOR + random.nextDouble() * 0.05
                : 1.0;
        double outlier = random.nextDouble() < OUTLIER_PROBABILITY ? OUTLIER_FACTOR : 1.0;

        double usd = base * country.costFactor() * department.payFactor() * variance * genderFactor * outlier;
        return BigDecimal.valueOf(usd).setScale(2, RoundingMode.HALF_UP);
    }

    /** Converts a USD target into what the employee is actually paid, rounded the way HR would. */
    private Money toLocalCurrency(BigDecimal usdAmount, CountrySeed country) {
        BigDecimal rate = fxRates.ratesToUsd().get(country.currency());
        BigDecimal local = usdAmount.divide(rate, 2, RoundingMode.HALF_UP);
        BigDecimal rounding = BigDecimal.valueOf(country.rounding());
        BigDecimal rounded = local.divide(rounding, 0, RoundingMode.HALF_UP).multiply(rounding);
        return Money.of(rounded.max(rounding), country.currency());
    }

    /**
     * Everyone reports to someone more senior in their own department. Executives report to nobody,
     * which is also how the org chart terminates.
     */
    private List<SeededEmployee> assignManagers(List<SeededEmployee> employees, Random random) {
        Map<String, Map<JobLevel, List<Long>>> byDepartmentAndLevel = new HashMap<>();
        for (SeededEmployee employee : employees) {
            byDepartmentAndLevel
                    .computeIfAbsent(employee.departmentName(), key -> new HashMap<>())
                    .computeIfAbsent(employee.level(), key -> new ArrayList<>())
                    .add(employee.id());
        }

        List<SeededEmployee> withManagers = new ArrayList<>(employees.size());
        for (SeededEmployee employee : employees) {
            withManagers.add(employee.withManager(
                    pickManager(byDepartmentAndLevel.get(employee.departmentName()), employee, random)));
        }
        return withManagers;
    }

    private Long pickManager(Map<JobLevel, List<Long>> departmentLevels, SeededEmployee employee, Random random) {
        JobLevel[] levels = JobLevel.values();
        for (int ordinal = employee.level().ordinal() + 1; ordinal < levels.length; ordinal++) {
            List<Long> candidates = departmentLevels.get(levels[ordinal]);
            if (candidates != null && !candidates.isEmpty()) {
                return candidates.get(random.nextInt(candidates.size()));
            }
        }
        return null;
    }

    private JobLevel pickLevel(Random random) {
        List<JobLevel> levels = List.of(JobLevel.values());
        return weightedPick(levels, levels.stream().map(SeedReferenceData.LEVEL_WEIGHTS::get).toList(), random);
    }

    private Gender pickGender(Random random) {
        double roll = random.nextDouble();
        if (roll < 0.45) {
            return Gender.FEMALE;
        }
        if (roll < 0.93) {
            return Gender.MALE;
        }
        return roll < 0.97 ? Gender.NON_BINARY : Gender.UNDISCLOSED;
    }

    private EmploymentStatus pickStatus(Random random) {
        double roll = random.nextDouble();
        if (roll < 0.93) {
            return EmploymentStatus.ACTIVE;
        }
        return roll < 0.97 ? EmploymentStatus.ON_LEAVE : EmploymentStatus.TERMINATED;
    }

    private LocalDate pickHireDate(Random random, LocalDate asOf) {
        // Tenure skewed towards recent hires, as it is in a growing company.
        double skewed = Math.pow(random.nextDouble(), 1.6);
        long daysBack = (long) (skewed * 365 * 11);
        return asOf.minusDays(daysBack + 30);
    }

    private String titleFor(DepartmentSeed department, JobLevel level, Random random) {
        String base = pick(department.titles(), random);
        return switch (level) {
            case L1 -> "Junior " + base;
            case L2 -> base;
            case L3 -> "Senior " + base;
            case L4 -> "Staff " + base;
            case L5 -> "Principal " + base;
            case L6 -> "Director, " + department.name();
            case L7 -> "VP, " + department.name();
        };
    }

    private String uniqueEmail(String firstName, String lastName, Set<String> used) {
        String base = (firstName + "." + lastName).toLowerCase();
        String candidate = base + "@acme.co";
        int suffix = 2;
        while (!used.add(candidate)) {
            candidate = base + suffix + "@acme.co";
            suffix++;
        }
        return candidate;
    }

    private <T> T pick(List<T> options, Random random) {
        return options.get(random.nextInt(options.size()));
    }

    private <T> T weightedPick(List<T> options, List<Double> weights, Random random) {
        double total = weights.stream().mapToDouble(Double::doubleValue).sum();
        double roll = random.nextDouble() * total;
        double cumulative = 0;
        for (int i = 0; i < options.size(); i++) {
            cumulative += weights.get(i);
            if (roll < cumulative) {
                return options.get(i);
            }
        }
        return options.get(options.size() - 1);
    }
}
