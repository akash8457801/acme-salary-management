package co.acme.salary.web.dto;

import co.acme.salary.domain.ChangeReason;
import co.acme.salary.domain.EmploymentStatus;
import co.acme.salary.domain.Gender;
import co.acme.salary.domain.JobLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * What the API accepts.
 *
 * <p>Validation here is structural — is this field present, is it a plausible email, is the amount
 * a positive number. Whether the change is <em>allowed</em> is a domain question and stays in
 * {@code CompensationTimeline}; bean validation cannot know that a raise must post-date the
 * salary it replaces.
 */
public final class ApiRequests {

    private ApiRequests() {
    }

    public record CreateEmployeeRequest(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email @Size(max = 160) String email,
            @NotNull Long departmentId,
            @NotBlank @Size(min = 2, max = 2) String countryCode,
            @NotBlank @Size(max = 120) String jobTitle,
            @NotNull JobLevel level,
            @NotNull Gender gender,
            Long managerId,
            @NotNull @PastOrPresent LocalDate hireDate,
            @NotNull @DecimalMin(value = "1", message = "Starting salary must be greater than zero")
            BigDecimal startingSalary) {
    }

    public record UpdateEmployeeRequest(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email @Size(max = 160) String email,
            @NotNull Long departmentId,
            @NotBlank @Size(max = 120) String jobTitle,
            @NotNull JobLevel level,
            Long managerId,
            @NotNull EmploymentStatus status) {
    }

    /**
     * A pay change. The currency is required rather than inferred so that a client cannot silently
     * post a number in the wrong currency and have the server guess right — the domain checks it
     * against the employee's payroll currency and rejects a mismatch.
     */
    public record CompensationChangeRequest(
            @NotNull @DecimalMin(value = "1", message = "Salary must be greater than zero") BigDecimal amount,
            @NotBlank @Size(min = 3, max = 3) String currency,
            @NotNull LocalDate effectiveFrom,
            @NotNull ChangeReason reason,
            @Size(max = 500) String note) {
    }
}
