package co.acme.salary.query;

/**
 * The columns the directory can be sorted by.
 *
 * <p>An allowlist rather than a free-text property name: sorting is the one place where a client
 * string would otherwise reach the query builder, and an enum makes that impossible while also
 * keeping the API honest about what is actually supported.
 */
public enum EmployeeSortKey {
    NAME,
    EMPLOYEE_CODE,
    SALARY,
    DEPARTMENT,
    COUNTRY,
    LEVEL,
    HIRE_DATE
}
