package co.acme.salary.config;

import co.acme.salary.domain.FxRateTable;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The two ambient dependencies the domain needs, made explicit so tests can replace them.
 *
 * <p>Nothing in this codebase calls {@code Instant.now()} or {@code LocalDate.now()} directly —
 * time arrives through the injected {@link Clock}. That is what lets a test assert on an exact
 * recorded-at timestamp instead of a range.
 */
@Configuration
public class DomainConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public FxRateTable fxRateTable() {
        return FxRateTable.CURRENT;
    }
}
