package co.acme.salary.query;

import co.acme.salary.domain.EmploymentStatus;
import co.acme.salary.domain.JobLevel;
import java.math.BigDecimal;

/**
 * What the HR manager has narrowed the directory down to. Every field is optional; a filter with
 * all fields null means "everyone".
 *
 * <p>Salary bounds are expressed in annualised USD, because that is the only figure comparable
 * across a directory that spans ten currencies.
 */
public record EmployeeFilter(
        String searchTerm,
        Long departmentId,
        String countryCode,
        JobLevel level,
        EmploymentStatus status,
        BigDecimal minAnnualUsd,
        BigDecimal maxAnnualUsd) {

    public static EmployeeFilter none() {
        return new EmployeeFilter(null, null, null, null, null, null, null);
    }

    public boolean hasSearchTerm() {
        return searchTerm != null && !searchTerm.isBlank();
    }

    /** Lower-cased and wrapped for a case-insensitive contains match. */
    public String searchPattern() {
        return "%" + searchTerm.trim().toLowerCase() + "%";
    }
}
