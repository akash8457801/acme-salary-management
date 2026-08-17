package co.acme.salary.domain;

/**
 * Recorded only to support the pay-equity report, which is the reason HR needs the field at all.
 * {@link #UNDISCLOSED} is a first-class value, not a null: people decline to answer, and the
 * report has to say so rather than quietly dropping them from the denominator.
 */
public enum Gender {
    FEMALE,
    MALE,
    NON_BINARY,
    UNDISCLOSED
}
