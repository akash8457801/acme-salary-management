package co.acme.salary.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Nested
    @DisplayName("normalises precision to the currency")
    class Precision {

        @Test
        void roundsToTheCurrencysFractionDigits() {
            assertThat(Money.of(new BigDecimal("1234.567"), "USD").amount())
                    .isEqualByComparingTo("1234.57");
        }

        @Test
        void dropsFractionsForCurrenciesThatHaveNone() {
            assertThat(Money.of(new BigDecimal("1234.56"), "JPY").amount())
                    .isEqualByComparingTo("1235");
        }

        @Test
        void twoAmountsThatDifferOnlyBelowCurrencyPrecisionAreEqual() {
            assertThat(Money.of(new BigDecimal("100.001"), "USD"))
                    .isEqualTo(Money.of(new BigDecimal("100.004"), "USD"));
        }
    }

    @Nested
    @DisplayName("refuses to mix currencies")
    class CurrencySafety {

        @Test
        void additionAcrossCurrenciesFails() {
            assertThatThrownBy(() -> Money.of(100, "USD").plus(Money.of(100, "EUR")))
                    .isInstanceOf(Money.CurrencyMismatchException.class)
                    .hasMessageContaining("USD")
                    .hasMessageContaining("EUR");
        }

        @Test
        void comparisonAcrossCurrenciesFails() {
            assertThatThrownBy(() -> Money.of(100, "USD").isGreaterThan(Money.of(1, "INR")))
                    .isInstanceOf(Money.CurrencyMismatchException.class);
        }
    }

    @Nested
    class Arithmetic {

        @Test
        void addsAndSubtractsWithinACurrency() {
            assertThat(Money.usd(90_000).plus(Money.usd(10_000))).isEqualTo(Money.usd(100_000));
            assertThat(Money.usd(90_000).minus(Money.usd(10_000))).isEqualTo(Money.usd(80_000));
        }

        @Test
        void describesARaiseAsAPercentage() {
            assertThat(Money.usd(55_000).percentageChangeFrom(Money.usd(50_000)))
                    .isEqualByComparingTo("10.00");
        }

        @Test
        void describesAPayCutAsANegativePercentage() {
            assertThat(Money.usd(45_000).percentageChangeFrom(Money.usd(50_000)))
                    .isEqualByComparingTo("-10.00");
        }

        @Test
        void refusesToComputeAPercentageChangeFromZero() {
            assertThatThrownBy(() -> Money.usd(50_000).percentageChangeFrom(Money.usd(0)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
