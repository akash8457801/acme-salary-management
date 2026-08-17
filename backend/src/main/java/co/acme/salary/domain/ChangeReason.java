package co.acme.salary.domain;

/**
 * Why a compensation record exists. Every change to pay must state a reason — "the number is
 * different now" is exactly the information a spreadsheet loses.
 */
public enum ChangeReason {

    /** The employee's starting salary. Always the first record on a timeline. */
    HIRE("Initial salary"),

    MERIT_INCREASE("Merit increase"),
    PROMOTION("Promotion"),
    MARKET_ADJUSTMENT("Market adjustment"),
    ROLE_CHANGE("Role change"),

    /** Fixing a value that was entered wrongly, as opposed to pay genuinely changing. */
    CORRECTION("Data correction");

    private final String label;

    ChangeReason(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
