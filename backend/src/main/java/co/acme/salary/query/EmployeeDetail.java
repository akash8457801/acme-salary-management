package co.acme.salary.query;

import co.acme.salary.domain.EmploymentStatus;
import co.acme.salary.domain.Gender;
import co.acme.salary.domain.JobLevel;
import java.time.LocalDate;
import java.util.List;

/** Everything the employee page shows: the person, and their whole compensation history. */
public record EmployeeDetail(
        Long id,
        String employeeCode,
        String firstName,
        String lastName,
        String email,
        Long departmentId,
        String departmentName,
        String countryCode,
        String countryName,
        String payrollCurrency,
        String jobTitle,
        JobLevel level,
        Gender gender,
        Long managerId,
        String managerName,
        LocalDate hireDate,
        EmploymentStatus status,
        List<CompensationEntry> compensationHistory) {

    public String fullName() {
        return firstName + " " + lastName;
    }

    /** The salary in force, i.e. the one open entry. */
    public CompensationEntry currentCompensation() {
        return compensationHistory.stream()
                .filter(CompensationEntry::current)
                .findFirst()
                .orElse(null);
    }
}
