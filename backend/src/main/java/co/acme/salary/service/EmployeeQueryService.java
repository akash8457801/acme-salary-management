package co.acme.salary.service;

import co.acme.salary.domain.Employee;
import co.acme.salary.query.CompensationEntry;
import co.acme.salary.query.EmployeeDetail;
import co.acme.salary.query.EmployeeFilter;
import co.acme.salary.query.EmployeeSortKey;
import co.acme.salary.query.EmployeeSummary;
import co.acme.salary.repository.CompensationRecordRepository;
import co.acme.salary.repository.EmployeeRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reading the directory.
 *
 * <p>Separated from {@link EmployeeService} because reads and writes have genuinely different
 * shapes here: writes work with entities and enforce invariants, reads project straight out of SQL
 * and never load an entity graph. Keeping them apart is what stops the list query from quietly
 * growing an N+1.
 */
@Service
public class EmployeeQueryService {

    /** A page this big already exceeds what any UI shows; the cap stops a client asking for 10,000. */
    static final int MAX_PAGE_SIZE = 200;

    private final EmployeeRepository employees;
    private final CompensationRecordRepository records;

    public EmployeeQueryService(EmployeeRepository employees, CompensationRecordRepository records) {
        this.employees = employees;
        this.records = records;
    }

    @Transactional(readOnly = true)
    public Page<EmployeeSummary> search(EmployeeFilter filter, EmployeeSortKey sortKey,
                                        Sort.Direction direction, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampPageSize(size));
        return employees.search(filter, sortKey, direction, pageable);
    }

    @Transactional(readOnly = true)
    public EmployeeDetail detail(Long employeeId) {
        Employee employee = employees.findById(employeeId)
                .orElseThrow(() -> ResourceNotFoundException.employee(employeeId));

        List<CompensationEntry> history = CompensationEntryAssembler.assemble(
                records.findByEmployeeIdOrderByEffectiveFromAsc(employeeId));

        return new EmployeeDetail(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getDepartment().getId(),
                employee.getDepartment().getName(),
                employee.getCountry().getCode(),
                employee.getCountry().getName(),
                employee.payrollCurrency(),
                employee.getJobTitle(),
                employee.getLevel(),
                employee.getGender(),
                employee.getManagerId(),
                managerName(employee.getManagerId()),
                employee.getHireDate(),
                employee.getStatus(),
                history);
    }

    /**
     * Walks the whole filtered result set a page at a time so an export of ten thousand rows never
     * sits in memory as one list.
     */
    @Transactional(readOnly = true)
    public void forEachMatching(EmployeeFilter filter, EmployeeSortKey sortKey,
                                Sort.Direction direction, java.util.function.Consumer<EmployeeSummary> consumer) {
        int page = 0;
        Page<EmployeeSummary> current;
        do {
            current = employees.search(filter, sortKey, direction, PageRequest.of(page, MAX_PAGE_SIZE));
            current.getContent().forEach(consumer);
            page++;
        } while (current.hasNext());
    }

    private String managerName(Long managerId) {
        if (managerId == null) {
            return null;
        }
        return employees.findById(managerId).map(Employee::fullName).orElse(null);
    }

    private int clampPageSize(int requested) {
        if (requested <= 0) {
            return 25;
        }
        return Math.min(requested, MAX_PAGE_SIZE);
    }
}
