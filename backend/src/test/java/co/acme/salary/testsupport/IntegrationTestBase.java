package co.acme.salary.testsupport;

import co.acme.salary.domain.Country;
import co.acme.salary.domain.Department;
import co.acme.salary.domain.FxRateTable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base for API tests: the full Spring context over an in-memory SQLite, with the clock frozen so
 * every {@code recordedAt} in every assertion is exact rather than "roughly now".
 *
 * <p>Each test starts from a truly empty database and creates precisely the employees it asserts
 * on. Tests that build their own world are longer to write but never fail because some other test
 * moved a shared fixture.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    public static final Instant FROZEN_NOW = Instant.parse("2026-08-01T09:00:00Z");

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FROZEN_NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected jakarta.persistence.EntityManager entityManager;

    @Autowired
    protected org.springframework.transaction.support.TransactionTemplate transactions;

    @Autowired
    protected FxRateTable fxRates;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("delete from compensation_record");
        jdbc.update("delete from employee");
        jdbc.update("delete from department");
        jdbc.update("delete from country");
    }

    /** Inserts reference rows and returns the department id tests hang employees off. */
    protected long givenEngineeringInGermanyAndUs() {
        return transactions.execute(tx -> {
            entityManager.persist(new Country("DE", "Germany", "EUR"));
            entityManager.persist(new Country("US", "United States", "USD"));
            Department engineering = new Department("Engineering");
            entityManager.persist(engineering);
            entityManager.flush();
            return engineering.getId();
        });
    }
}
