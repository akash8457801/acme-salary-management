package co.acme.salary.web;

import co.acme.salary.domain.EmploymentStatus;
import co.acme.salary.domain.JobLevel;
import co.acme.salary.domain.Money;
import co.acme.salary.query.CompensationEntry;
import co.acme.salary.query.EmployeeFilter;
import co.acme.salary.query.EmployeeSortKey;
import co.acme.salary.query.EmployeeSummary;
import co.acme.salary.service.CompensationService;
import co.acme.salary.service.EmployeeCommands.EmployeeUpdate;
import co.acme.salary.service.EmployeeCommands.NewEmployee;
import co.acme.salary.service.EmployeeQueryService;
import co.acme.salary.service.EmployeeService;
import co.acme.salary.web.dto.ApiDtos.CompensationEntryDto;
import co.acme.salary.web.dto.ApiDtos.EmployeeDetailDto;
import co.acme.salary.web.dto.ApiDtos.EmployeeSummaryDto;
import co.acme.salary.web.dto.ApiDtos.PageDto;
import co.acme.salary.web.dto.ApiRequests.CompensationChangeRequest;
import co.acme.salary.web.dto.ApiRequests.CreateEmployeeRequest;
import co.acme.salary.web.dto.ApiRequests.UpdateEmployeeRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeQueryService queries;
    private final EmployeeService employees;
    private final CompensationService compensation;

    public EmployeeController(EmployeeQueryService queries, EmployeeService employees,
                              CompensationService compensation) {
        this.queries = queries;
        this.employees = employees;
        this.compensation = compensation;
    }

    /**
     * The directory. Every parameter is optional; filtering, sorting and paging all happen in SQL,
     * so this endpoint costs the same whether ACME has 100 employees or 100,000.
     */
    @GetMapping
    public PageDto<EmployeeSummaryDto> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) JobLevel level,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(required = false) BigDecimal minAnnualUsd,
            @RequestParam(required = false) BigDecimal maxAnnualUsd,
            @RequestParam(defaultValue = "NAME") EmployeeSortKey sort,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {

        EmployeeFilter filter = new EmployeeFilter(
                q, departmentId, countryCode, level, status, minAnnualUsd, maxAnnualUsd);

        return PageDto.from(queries.search(filter, sort, direction, page, size), EmployeeSummaryDto::from);
    }

    @GetMapping("/{id}")
    public EmployeeDetailDto detail(@PathVariable Long id) {
        return EmployeeDetailDto.from(queries.detail(id));
    }

    @PostMapping
    public ResponseEntity<EmployeeDetailDto> hire(@Valid @RequestBody CreateEmployeeRequest request) {
        Long id = employees.hire(new NewEmployee(
                request.firstName(), request.lastName(), request.email(), request.departmentId(),
                request.countryCode().toUpperCase(), request.jobTitle(), request.level(), request.gender(),
                request.managerId(), request.hireDate(), request.startingSalary()));

        return ResponseEntity.created(URI.create("/api/employees/" + id))
                .body(EmployeeDetailDto.from(queries.detail(id)));
    }

    @PutMapping("/{id}")
    public EmployeeDetailDto update(@PathVariable Long id, @Valid @RequestBody UpdateEmployeeRequest request) {
        employees.update(id, new EmployeeUpdate(
                request.firstName(), request.lastName(), request.email(), request.departmentId(),
                request.jobTitle(), request.level(), request.managerId(), request.status()));
        return EmployeeDetailDto.from(queries.detail(id));
    }

    @GetMapping("/{id}/compensation")
    public List<CompensationEntryDto> compensationHistory(@PathVariable Long id) {
        return compensation.history(id).stream().map(CompensationEntryDto::from).toList();
    }

    /**
     * Records a pay change. Deliberately a POST that appends rather than a PUT that replaces:
     * the salary is not being corrected, it is being superseded, and both values remain true of
     * their own period.
     */
    @PostMapping("/{id}/compensation")
    public ResponseEntity<CompensationEntryDto> recordChange(
            @PathVariable Long id, @Valid @RequestBody CompensationChangeRequest request) {

        CompensationEntry appended = compensation.recordChange(
                id,
                Money.of(request.amount(), request.currency().toUpperCase()),
                request.effectiveFrom(),
                request.reason(),
                request.note());

        return ResponseEntity.created(URI.create("/api/employees/" + id + "/compensation"))
                .body(CompensationEntryDto.from(appended));
    }

    /** The current view as a CSV, because Finance will always want a spreadsheet of something. */
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> export(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) JobLevel level,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(required = false) BigDecimal minAnnualUsd,
            @RequestParam(required = false) BigDecimal maxAnnualUsd,
            @RequestParam(defaultValue = "NAME") EmployeeSortKey sort,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {

        EmployeeFilter filter = new EmployeeFilter(
                q, departmentId, countryCode, level, status, minAnnualUsd, maxAnnualUsd);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header("Content-Disposition", "attachment; filename=\"acme-salaries.csv\"")
                .body(outputStream -> {
                    try (CsvWriter csv = new CsvWriter(outputStream)) {
                        csv.writeRow("Employee code", "Name", "Email", "Department", "Country",
                                "Job title", "Level", "Status", "Hire date", "Salary", "Currency",
                                "Annual USD");
                        queries.forEachMatching(filter, sort, direction, summary -> writeRow(csv, summary));
                    }
                });
    }

    private void writeRow(CsvWriter csv, EmployeeSummary summary) {
        csv.writeRow(
                summary.employeeCode(),
                summary.fullName(),
                summary.email(),
                summary.departmentName(),
                summary.countryName(),
                summary.jobTitle(),
                summary.level().name(),
                summary.status().name(),
                String.valueOf(summary.hireDate()),
                summary.salaryAmount() == null ? "" : summary.salaryAmount().toPlainString(),
                summary.currencyCode() == null ? "" : summary.currencyCode(),
                summary.annualUsdAmount() == null ? "" : summary.annualUsdAmount().toPlainString());
    }
}
