package co.acme.salary.domain;

public enum EmploymentStatus {
    ACTIVE,
    ON_LEAVE,
    TERMINATED;

    /** Only active and on-leave employees count towards payroll cost. */
    public boolean countsTowardsPayroll() {
        return this != TERMINATED;
    }
}
