package co.acme.salary.seed;

import co.acme.salary.domain.ChangeReason;
import co.acme.salary.domain.EmploymentStatus;
import co.acme.salary.domain.Gender;
import co.acme.salary.domain.JobLevel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Plain data produced by {@link SeedDataGenerator}, before anything touches a database. */
public final class SeedData {

    private SeedData() {
    }

    public record SeededEmployee(
            long id,
            String employeeCode,
            String firstName,
            String lastName,
            String email,
            String departmentName,
            String countryCode,
            String jobTitle,
            JobLevel level,
            Gender gender,
            Long managerId,
            LocalDate hireDate,
            EmploymentStatus status,
            List<SeededCompensation> compensation) {

        public SeededEmployee withManager(Long managerId) {
            return new SeededEmployee(id, employeeCode, firstName, lastName, email, departmentName,
                    countryCode, jobTitle, level, gender, managerId, hireDate, status, compensation);
        }

        public SeededCompensation currentCompensation() {
            return compensation.get(compensation.size() - 1);
        }
    }

    /**
     * A pay change as the generator describes it: an amount, a date and a reason. Closing the
     * previous period and converting to USD are not the generator's business — the seeder feeds
     * these through {@code CompensationTimeline}, so seeded history obeys exactly the same
     * invariants as history entered through the UI.
     */
    public record SeededCompensation(
            BigDecimal amount,
            String currencyCode,
            LocalDate effectiveFrom,
            ChangeReason reason,
            String note) {
    }
}
