package co.acme.salary.service;

import co.acme.salary.domain.EmploymentStatus;
import co.acme.salary.domain.Gender;
import co.acme.salary.domain.JobLevel;
import java.math.BigDecimal;
import java.time.LocalDate;

/** The write operations the HR manager can perform, as data. */
public final class EmployeeCommands {

    private EmployeeCommands() {
    }

    /**
     * Hiring someone. The starting salary is part of the command rather than a follow-up call:
     * an employee with no compensation record is not a state this system should be able to reach.
     * The amount is in the country's payroll currency.
     */
    public record NewEmployee(
            String firstName,
            String lastName,
            String email,
            Long departmentId,
            String countryCode,
            String jobTitle,
            JobLevel level,
            Gender gender,
            Long managerId,
            LocalDate hireDate,
            BigDecimal startingSalary) {
    }

    /**
     * Editing someone's profile. Country is absent on purpose — moving an employee to another
     * country changes their payroll currency, which is a compensation event and has to go through
     * the timeline rather than a profile edit.
     */
    public record EmployeeUpdate(
            String firstName,
            String lastName,
            String email,
            Long departmentId,
            String jobTitle,
            JobLevel level,
            Long managerId,
            EmploymentStatus status) {
    }
}
