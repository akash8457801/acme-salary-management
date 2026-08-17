package co.acme.salary.domain;

/**
 * ACME's job levels, from graduate to executive. Ordinal position is meaningful — a level is
 * senior to another if it appears later — so the order of these constants is part of the model.
 */
public enum JobLevel {

    L1("Associate"),
    L2("Engineer"),
    L3("Senior"),
    L4("Staff"),
    L5("Principal"),
    L6("Director"),
    L7("Executive");

    private final String title;

    JobLevel(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }

    public boolean isSeniorTo(JobLevel other) {
        return ordinal() > other.ordinal();
    }
}
