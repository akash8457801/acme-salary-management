package co.acme.salary.query;

import co.acme.salary.domain.EmploymentStatus;
import co.acme.salary.domain.JobLevel;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row of the employee directory: enough to render the list, and nothing more.
 *
 * <p>This is a read model built directly by the database, not a mapped entity. The list query is
 * the hottest path in the application and there is no reason to hydrate ten thousand object graphs
 * to show fifty rows.
 *
 * <p>Salary fields are nullable: an employee can exist before their first compensation record has
 * been entered, and the UI should show that honestly rather than a misleading zero.
 */
public record EmployeeSummary(
        Long id,
        String employeeCode,
        String firstName,
        String lastName,
        String email,
        Long departmentId,
        String departmentName,
        String countryCode,
        String countryName,
        String jobTitle,
        JobLevel level,
        EmploymentStatus status,
        LocalDate hireDate,
        BigDecimal salaryAmount,
        String currencyCode,
        BigDecimal annualUsdAmount) {

    public String fullName() {
        return firstName + " " + lastName;
    }
}
