package co.acme.salary.seed;

import co.acme.salary.domain.CompensationRecord;
import co.acme.salary.domain.CompensationTimeline;
import co.acme.salary.domain.Country;
import co.acme.salary.domain.Department;
import co.acme.salary.domain.Employee;
import co.acme.salary.domain.FxRateTable;
import co.acme.salary.domain.Money;
import co.acme.salary.seed.SeedData.SeededCompensation;
import co.acme.salary.seed.SeedData.SeededEmployee;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Populates an empty database with ACME as it would be on day one of using this software.
 *
 * <p>Two properties of this seeder matter more than its speed:
 *
 * <ul>
 *   <li><b>It only ever runs against an empty database.</b> Re-running the application does not
 *       duplicate, reset or "fix up" anything — a seeder that can overwrite real data is a
 *       production incident waiting to happen.</li>
 *   <li><b>It writes through the domain, not around it.</b> Every salary goes through
 *       {@link CompensationTimeline}, so ten thousand seeded histories obey exactly the same
 *       invariants as one entered by hand. Bulk-inserting rows would have been faster and would
 *       have let inconsistent history in through the back door.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "salary.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);
    private static final int FLUSH_EVERY = 500;

    private final EntityManager entityManager;
    private final JdbcTemplate jdbc;
    private final FxRateTable fxRates;
    private final Clock clock;
    private final int employeeCount;

    public DatabaseSeeder(EntityManager entityManager, JdbcTemplate jdbc, FxRateTable fxRates, Clock clock,
                          @Value("${salary.seed.employee-count:10000}") int employeeCount) {
        this.entityManager = entityManager;
        this.jdbc = jdbc;
        this.fxRates = fxRates;
        this.clock = clock;
        this.employeeCount = employeeCount;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Long existing = entityManager.createQuery("select count(e) from Employee e", Long.class)
                .getSingleResult();
        if (existing > 0) {
            log.info("Database already holds {} employees — skipping seed", existing);
            return;
        }

        long startedAt = System.currentTimeMillis();
        log.info("Seeding {} employees...", employeeCount);

        Map<String, Country> countries = persistCountries();
        Map<String, Department> departments = persistDepartments();

        List<SeededEmployee> seeded =
                new SeedDataGenerator(fxRates).generate(employeeCount, LocalDate.now(clock));

        Map<Long, Long> seedIdToDatabaseId = persistEmployees(seeded, countries, departments);
        linkManagers(seeded, seedIdToDatabaseId);

        log.info("Seeded {} employees in {} ms", seeded.size(), System.currentTimeMillis() - startedAt);
    }

    private Map<String, Country> persistCountries() {
        Map<String, Country> countries = new HashMap<>();
        SeedReferenceData.COUNTRIES.forEach(seed -> {
            Country country = new Country(seed.code(), seed.name(), seed.currency());
            entityManager.persist(country);
            countries.put(seed.code(), country);
        });
        return countries;
    }

    private Map<String, Department> persistDepartments() {
        Map<String, Department> departments = new HashMap<>();
        SeedReferenceData.DEPARTMENTS.forEach(seed -> {
            Department department = new Department(seed.name());
            entityManager.persist(department);
            departments.put(seed.name(), department);
        });
        return departments;
    }

    private Map<Long, Long> persistEmployees(List<SeededEmployee> seeded, Map<String, Country> countries,
                                             Map<String, Department> departments) {
        Map<Long, Long> seedIdToDatabaseId = new HashMap<>(seeded.size());
        int written = 0;

        for (SeededEmployee seed : seeded) {
            Employee employee = new Employee(
                    seed.employeeCode(), seed.firstName(), seed.lastName(), seed.email(),
                    departments.get(seed.departmentName()), countries.get(seed.countryCode()),
                    seed.jobTitle(), seed.level(), seed.gender(), null, seed.hireDate(), seed.status());
            entityManager.persist(employee);
            seedIdToDatabaseId.put(seed.id(), employee.getId());

            persistCompensationHistory(employee, seed.compensation());

            if (++written % FLUSH_EVERY == 0) {
                entityManager.flush();
                entityManager.clear();
                // The entities cached above are detached by clear(); nothing below reads them.
                countries = refresh(countries, Country.class, Country::getCode);
                departments = refreshDepartments(departments);
            }
        }
        entityManager.flush();
        return seedIdToDatabaseId;
    }

    private void persistCompensationHistory(Employee employee, List<SeededCompensation> history) {
        CompensationTimeline timeline = CompensationTimeline.of(employee, List.of());
        for (SeededCompensation change : history) {
            timeline.recordChange(
                    Money.of(change.amount(), change.currencyCode()),
                    change.effectiveFrom(),
                    change.reason(),
                    change.note(),
                    fxRates,
                    clock.instant());
        }
        for (CompensationRecord record : timeline.records()) {
            entityManager.persist(record);
        }
    }

    /**
     * Manager ids are resolved in a second pass because a manager may be generated after the
     * person reporting to them. A single batched UPDATE keeps that from becoming ten thousand
     * round trips.
     */
    private void linkManagers(List<SeededEmployee> seeded, Map<Long, Long> seedIdToDatabaseId) {
        List<Object[]> updates = new ArrayList<>();
        for (SeededEmployee seed : seeded) {
            if (seed.managerId() == null) {
                continue;
            }
            updates.add(new Object[]{
                    seedIdToDatabaseId.get(seed.managerId()),
                    seedIdToDatabaseId.get(seed.id())});
        }
        entityManager.flush();
        jdbc.batchUpdate("update employee set manager_id = ? where id = ?", updates);
    }

    private <T> Map<String, T> refresh(Map<String, T> detached, Class<T> type,
                                       java.util.function.Function<T, String> key) {
        Map<String, T> reattached = new HashMap<>(detached.size());
        detached.values().forEach(value -> {
            T managed = entityManager.find(type, key.apply(value));
            reattached.put(key.apply(managed), managed);
        });
        return reattached;
    }

    private Map<String, Department> refreshDepartments(Map<String, Department> detached) {
        Map<String, Department> reattached = new HashMap<>(detached.size());
        detached.values().forEach(department -> {
            Department managed = entityManager.find(Department.class, department.getId());
            reattached.put(managed.getName(), managed);
        });
        return reattached;
    }
}
