package co.acme.salary.domain;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A versioned set of exchange rates into USD, the reporting currency.
 *
 * <p>Rates are a fixed, versioned table rather than a live feed. That is a deliberate product
 * decision (see ADR-004): a compensation report has to be reproducible, so the rate that converted
 * a record is pinned to the record. Every conversion records {@link #version()} so a future
 * restatement job can find exactly which numbers were produced by which table.
 */
public record FxRateTable(String version, Map<String, BigDecimal> ratesToUsd) {

    /**
     * The rates this build ships with. Approximate mid-market rates; precision beyond four decimal
     * places is meaningless for salary planning.
     */
    public static final FxRateTable CURRENT = new FxRateTable("2026-01", Map.of(
            "USD", new BigDecimal("1.0000"),
            "EUR", new BigDecimal("1.0850"),
            "GBP", new BigDecimal("1.2700"),
            "INR", new BigDecimal("0.0120"),
            "SGD", new BigDecimal("0.7450"),
            "AUD", new BigDecimal("0.6600"),
            "CAD", new BigDecimal("0.7300"),
            "BRL", new BigDecimal("0.1750"),
            "PLN", new BigDecimal("0.2500"),
            "JPY", new BigDecimal("0.0066")));

    public FxRateTable {
        ratesToUsd = Collections.unmodifiableMap(new LinkedHashMap<>(ratesToUsd));
    }

    public Set<String> supportedCurrencies() {
        return ratesToUsd.keySet();
    }

    public boolean supports(String currencyCode) {
        return ratesToUsd.containsKey(currencyCode);
    }

    /** Converts to USD; {@link Money} rounds the result to the currency's own precision. */
    public Money toUsd(Money money) {
        BigDecimal rate = ratesToUsd.get(money.currencyCode());
        if (rate == null) {
            throw new UnsupportedCurrencyException(money.currencyCode(), version);
        }
        return Money.of(money.amount().multiply(rate), "USD");
    }

    /** Thrown when a salary is recorded in a currency the current rate table does not cover. */
    public static class UnsupportedCurrencyException extends IllegalArgumentException {
        public UnsupportedCurrencyException(String currencyCode, String tableVersion) {
            super("Currency %s is not in FX rate table %s".formatted(currencyCode, tableVersion));
        }
    }
}
