package co.acme.salary.query;

/**
 * The dimensions payroll can be sliced by.
 *
 * <p>The SQL fragments live on the enum rather than being assembled from a request parameter: the
 * insights queries are hand-written SQL, and this is what keeps a client-supplied string from ever
 * reaching one.
 */
public enum BreakdownDimension {

    DEPARTMENT("cast(d.id as text)", "d.name"),
    COUNTRY("c.code", "c.name"),
    LEVEL("e.job_level", "e.job_level");

    private final String keyExpression;
    private final String labelExpression;

    BreakdownDimension(String keyExpression, String labelExpression) {
        this.keyExpression = keyExpression;
        this.labelExpression = labelExpression;
    }

    public String keyExpression() {
        return keyExpression;
    }

    public String labelExpression() {
        return labelExpression;
    }
}
