package co.acme.salary.web.dto;

import co.acme.salary.query.CompensationEntry;
import co.acme.salary.query.EmployeeDetail;
import co.acme.salary.query.EmployeeSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * What the API sends back.
 *
 * <p>These are separate from the query read models on purpose. The read models are shaped by what
 * SQL can produce efficiently; the API is shaped by what a client wants to render — money as an
 * amount-plus-currency pair rather than two loose columns, levels carrying their human title.
 * Keeping them apart means a change to one query cannot silently reshape the public contract.
 */
public final class ApiDtos {

    private ApiDtos() {
    }

    public record MoneyDto(BigDecimal amount, String currency) {

        static MoneyDto of(BigDecimal amount, String currency) {
            return amount == null ? null : new MoneyDto(amount, currency);
        }
    }

    public record EmployeeSummaryDto(
            Long id,
            String employeeCode,
            String fullName,
            String email,
            Long departmentId,
            String department,
            String countryCode,
            String country,
            String jobTitle,
            String level,
            String levelTitle,
            String status,
            LocalDate hireDate,
            MoneyDto salary,
            MoneyDto annualUsd) {

        public static EmployeeSummaryDto from(EmployeeSummary summary) {
            return new EmployeeSummaryDto(
                    summary.id(),
                    summary.employeeCode(),
                    summary.fullName(),
                    summary.email(),
                    summary.departmentId(),
                    summary.departmentName(),
                    summary.countryCode(),
                    summary.countryName(),
                    summary.jobTitle(),
                    summary.level().name(),
                    summary.level().title(),
                    summary.status().name(),
                    summary.hireDate(),
                    MoneyDto.of(summary.salaryAmount(), summary.currencyCode()),
                    MoneyDto.of(summary.annualUsdAmount(), "USD"));
        }
    }

    public record CompensationEntryDto(
            Long id,
            MoneyDto salary,
            MoneyDto annualUsd,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String reason,
            String reasonLabel,
            String note,
            Instant recordedAt,
            BigDecimal changePercent,
            boolean current) {

        public static CompensationEntryDto from(CompensationEntry entry) {
            return new CompensationEntryDto(
                    entry.id(),
                    MoneyDto.of(entry.amount(), entry.currencyCode()),
                    MoneyDto.of(entry.annualUsdAmount(), "USD"),
                    entry.effectiveFrom(),
                    entry.effectiveTo(),
                    entry.reason().name(),
                    entry.reasonLabel(),
                    entry.note(),
                    entry.recordedAt(),
                    entry.changePercent(),
                    entry.current());
        }
    }

    public record EmployeeDetailDto(
            Long id,
            String employeeCode,
            String firstName,
            String lastName,
            String fullName,
            String email,
            Long departmentId,
            String department,
            String countryCode,
            String country,
            String payrollCurrency,
            String jobTitle,
            String level,
            String levelTitle,
            String gender,
            Long managerId,
            String managerName,
            LocalDate hireDate,
            String status,
            CompensationEntryDto currentCompensation,
            List<CompensationEntryDto> compensationHistory) {

        public static EmployeeDetailDto from(EmployeeDetail detail) {
            CompensationEntry current = detail.currentCompensation();
            return new EmployeeDetailDto(
                    detail.id(),
                    detail.employeeCode(),
                    detail.firstName(),
                    detail.lastName(),
                    detail.fullName(),
                    detail.email(),
                    detail.departmentId(),
                    detail.departmentName(),
                    detail.countryCode(),
                    detail.countryName(),
                    detail.payrollCurrency(),
                    detail.jobTitle(),
                    detail.level().name(),
                    detail.level().title(),
                    detail.gender().name(),
                    detail.managerId(),
                    detail.managerName(),
                    detail.hireDate(),
                    detail.status().name(),
                    current == null ? null : CompensationEntryDto.from(current),
                    detail.compensationHistory().stream()
                            .sorted((left, right) -> right.effectiveFrom().compareTo(left.effectiveFrom()))
                            .map(CompensationEntryDto::from)
                            .toList());
        }
    }

    /** A page of results, flattened — Spring's own {@code Page} serialises into an unstable shape. */
    public record PageDto<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {

        public static <S, T> PageDto<T> from(Page<S> page, java.util.function.Function<S, T> mapper) {
            return new PageDto<>(
                    page.getContent().stream().map(mapper).toList(),
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages());
        }
    }

    /** A name/value pair for the UI's dropdowns. */
    public record OptionDto(String value, String label) {
    }

    public record ReferenceDataDto(
            List<DepartmentDto> departments,
            List<CountryDto> countries,
            List<OptionDto> levels,
            List<OptionDto> statuses,
            List<OptionDto> changeReasons,
            List<OptionDto> genders) {
    }

    public record DepartmentDto(Long id, String name) {
    }

    public record CountryDto(String code, String name, String currency) {
    }
}
