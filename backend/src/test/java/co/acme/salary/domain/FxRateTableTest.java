package co.acme.salary.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FxRateTableTest {

    private static final FxRateTable RATES = new FxRateTable("test-1", Map.of(
            "USD", new BigDecimal("1.0000"),
            "EUR", new BigDecimal("1.1000")));

    @Test
    void convertsIntoTheReportingCurrency() {
        assertThat(RATES.toUsd(Money.of(100_000, "EUR")))
                .isEqualTo(Money.of(new BigDecimal("110000.00"), "USD"));
    }

    @Test
    void leavesTheReportingCurrencyUntouched() {
        assertThat(RATES.toUsd(Money.usd(90_000))).isEqualTo(Money.usd(90_000));
    }

    @Test
    void rejectsACurrencyItHasNoRateFor() {
        assertThatThrownBy(() -> RATES.toUsd(Money.of(1_000_000, "INR")))
                .isInstanceOf(FxRateTable.UnsupportedCurrencyException.class)
                .hasMessageContaining("INR")
                .hasMessageContaining("test-1");
    }

    @Test
    void isImmutableSoAConvertedRecordStaysExplainable() {
        Map<String, BigDecimal> mutable = new java.util.HashMap<>(Map.of("USD", BigDecimal.ONE));
        FxRateTable table = new FxRateTable("v1", mutable);

        mutable.put("EUR", new BigDecimal("2"));

        assertThat(table.supports("EUR")).isFalse();
    }

    @Test
    void shippedTableCoversEveryCurrencyWeEmployPeopleIn() {
        assertThat(FxRateTable.CURRENT.supportedCurrencies())
                .contains("USD", "EUR", "GBP", "INR", "SGD", "AUD", "CAD", "BRL", "PLN");
    }
}
