package co.acme.salary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A person ACME employs.
 *
 * <p>Note what is <em>not</em> here: there is no salary field. Current pay is derived from the
 * employee's {@link CompensationRecord} timeline. A mutable salary column is the spreadsheet
 * failure mode this product exists to fix — the moment you overwrite it, the previous value and
 * the reason it changed are gone. See ADR-003.
 */
@Entity
@Table(name = "employee", indexes = {
        @Index(name = "idx_employee_department", columnList = "department_id"),
        @Index(name = "idx_employee_country", columnList = "country_code"),
        @Index(name = "idx_employee_level", columnList = "job_level"),
        @Index(name = "idx_employee_status", columnList = "status"),
        @Index(name = "idx_employee_last_name", columnList = "last_name")
})
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-facing identifier, e.g. ACME-01234. Stable across name changes. */
    @Column(name = "employee_code", nullable = false, unique = true, length = 16)
    private String employeeCode;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_code", nullable = false)
    private Country country;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_level", nullable = false, length = 2)
    private JobLevel level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Gender gender;

    /**
     * Deliberately a raw id rather than a self-referencing association: the only thing this system
     * does with a manager is show a name and (eventually) roll up a team. A mapped {@code @ManyToOne}
     * invites accidental traversal of the whole org chart on a list query.
     */
    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EmploymentStatus status;

    /**
     * Mapped only so that the directory query can join to the open compensation record with a
     * criteria {@code ON} clause. It is lazy and intentionally not exposed by a getter — the
     * timeline is loaded through {@code CompensationRecordRepository}, never by traversal.
     */
    @OneToMany(mappedBy = "employee", fetch = FetchType.LAZY)
    private List<CompensationRecord> compensationRecords = new ArrayList<>();

    protected Employee() {
        // for JPA
    }

    public Employee(String employeeCode, String firstName, String lastName, String email,
                    Department department, Country country, String jobTitle, JobLevel level,
                    Gender gender, Long managerId, LocalDate hireDate, EmploymentStatus status) {
        this.employeeCode = employeeCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.department = department;
        this.country = country;
        this.jobTitle = jobTitle;
        this.level = level;
        this.gender = gender;
        this.managerId = managerId;
        this.hireDate = hireDate;
        this.status = status;
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    /** The currency this employee is paid in, which follows from where they are employed. */
    public String payrollCurrency() {
        return country.getCurrencyCode();
    }

    public void updateProfile(String firstName, String lastName, String email, Department department,
                              String jobTitle, JobLevel level, Long managerId, EmploymentStatus status) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.department = department;
        this.jobTitle = jobTitle;
        this.level = level;
        this.managerId = managerId;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public Department getDepartment() {
        return department;
    }

    public Country getCountry() {
        return country;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public JobLevel getLevel() {
        return level;
    }

    public Gender getGender() {
        return gender;
    }

    public Long getManagerId() {
        return managerId;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public EmploymentStatus getStatus() {
        return status;
    }
}
