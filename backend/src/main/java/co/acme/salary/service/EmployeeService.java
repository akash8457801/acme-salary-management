package co.acme.salary.service;

import co.acme.salary.domain.ChangeReason;
import co.acme.salary.domain.Country;
import co.acme.salary.domain.Department;
import co.acme.salary.domain.Employee;
import co.acme.salary.domain.EmploymentStatus;
import co.acme.salary.domain.Money;
import co.acme.salary.repository.CountryRepository;
import co.acme.salary.repository.DepartmentRepository;
import co.acme.salary.repository.EmployeeRepository;
import co.acme.salary.service.EmployeeCommands.EmployeeUpdate;
import co.acme.salary.service.EmployeeCommands.NewEmployee;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creating and editing employees. */
@Service
public class EmployeeService {

    private static final String CODE_PREFIX = "ACME-";
    private static final int CODE_DIGITS = 5;

    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final CountryRepository countries;
    private final CompensationService compensation;

    public EmployeeService(EmployeeRepository employees, DepartmentRepository departments,
                           CountryRepository countries, CompensationService compensation) {
        this.employees = employees;
        this.departments = departments;
        this.countries = countries;
        this.compensation = compensation;
    }

    /**
     * Hires someone: creates the person and their opening salary in one transaction, so the
     * "employee with no salary" state never exists, not even briefly.
     */
    @Transactional
    public Long hire(NewEmployee command) {
        if (employees.existsByEmail(command.email())) {
            throw new DuplicateEmailException(command.email());
        }
        Department department = department(command.departmentId());
        Country country = country(command.countryCode());
        requireManagerExists(command.managerId());

        Employee employee = new Employee(
                nextEmployeeCode(),
                command.firstName().trim(),
                command.lastName().trim(),
                command.email().trim().toLowerCase(),
                department,
                country,
                command.jobTitle().trim(),
                command.level(),
                command.gender(),
                command.managerId(),
                command.hireDate(),
                EmploymentStatus.ACTIVE);

        Long id = employees.save(employee).getId();

        compensation.recordChange(
                id,
                Money.of(command.startingSalary(), country.getCurrencyCode()),
                command.hireDate(),
                ChangeReason.HIRE,
                "Starting salary");

        return id;
    }

    @Transactional
    public void update(Long employeeId, EmployeeUpdate command) {
        Employee employee = employees.findById(employeeId)
                .orElseThrow(() -> ResourceNotFoundException.employee(employeeId));

        if (employees.existsByEmailAndIdNot(command.email().trim().toLowerCase(), employeeId)) {
            throw new DuplicateEmailException(command.email());
        }
        requireManagerExists(command.managerId());
        if (employeeId.equals(command.managerId())) {
            throw new IllegalArgumentException("An employee cannot be their own manager");
        }

        employee.updateProfile(
                command.firstName().trim(),
                command.lastName().trim(),
                command.email().trim().toLowerCase(),
                department(command.departmentId()),
                command.jobTitle().trim(),
                command.level(),
                command.managerId(),
                command.status());
    }

    private Department department(Long id) {
        return departments.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No department with id " + id));
    }

    private Country country(String code) {
        return countries.findById(code)
                .orElseThrow(() -> new ResourceNotFoundException("ACME does not employ people in " + code));
    }

    private void requireManagerExists(Long managerId) {
        if (managerId != null && !employees.existsById(managerId)) {
            throw new ResourceNotFoundException("No manager with id " + managerId);
        }
    }

    /**
     * Codes are sequential and zero-padded so they sort naturally and read well on screen. The
     * unique constraint on the column is the real guard; this is the allocator, not the guarantee.
     */
    private String nextEmployeeCode() {
        long next = employees.findHighestEmployeeCode()
                .map(code -> Long.parseLong(code.substring(CODE_PREFIX.length())) + 1)
                .orElse(1L);
        return CODE_PREFIX + String.format("%0" + CODE_DIGITS + "d", next);
    }

    /** Two employees cannot share a work email; it is the closest thing to a natural key here. */
    public static class DuplicateEmailException extends RuntimeException {
        public DuplicateEmailException(String email) {
            super(email + " is already in use by another employee");
        }
    }
}
