package co.acme.salary.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * An amount of money in a specific currency.
 *
 * <p>Salaries in this system are always annual base amounts. The amount is normalised to the
 * currency's own number of fractional digits on construction, so a {@code Money} never carries
 * more precision than the currency can actually express.
 *
 * <p>Two amounts in different currencies are not comparable and not addable. That restriction is
 * the point of the type: it is what stops a European salary from being silently summed into a
 * dollar total.
 */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    public static Money of(long amount, String currencyCode) {
        return of(BigDecimal.valueOf(amount), currencyCode);
    }

    public static Money usd(long amount) {
        return of(amount, "USD");
    }

    public String currencyCode() {
        return currency.getCurrencyCode();
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money multipliedBy(BigDecimal factor) {
        return new Money(amount.multiply(factor), currency);
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    /**
     * Percentage change from {@code previous} to this amount, e.g. 50000 -> 55000 gives 10.00.
     * Used to describe a raise; both amounts must be in the same currency.
     */
    public BigDecimal percentageChangeFrom(Money previous) {
        requireSameCurrency(previous);
        if (previous.amount.signum() == 0) {
            throw new IllegalArgumentException("Cannot compute a percentage change from zero");
        }
        return amount.subtract(previous.amount)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.amount, 2, RoundingMode.HALF_UP);
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + amount.toPlainString();
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other");
        if (!currency.equals(other.currency)) {
            throw new CurrencyMismatchException(currency, other.currency);
        }
    }

    /** Thrown when two amounts in different currencies are combined or compared. */
    public static class CurrencyMismatchException extends IllegalArgumentException {
        public CurrencyMismatchException(Currency left, Currency right) {
            super("Cannot combine %s with %s".formatted(left.getCurrencyCode(), right.getCurrencyCode()));
        }
    }
}
