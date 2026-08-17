package co.acme.salary.testsupport;

import co.acme.salary.domain.Country;
import co.acme.salary.domain.Department;
import co.acme.salary.domain.Employee;
import co.acme.salary.domain.EmploymentStatus;
import co.acme.salary.domain.Gender;
import co.acme.salary.domain.JobLevel;
import java.time.LocalDate;

/**
 * Fixtures for tests. Everything is fixed — no random values, no {@code LocalDate.now()} — so a
 * failing test fails the same way tomorrow.
 */
public final class Fixtures {

    public static final Country GERMANY = new Country("DE", "Germany", "EUR");
    public static final Country UNITED_STATES = new Country("US", "United States", "USD");
    public static final Country INDIA = new Country("IN", "India", "INR");
    public static final Department ENGINEERING = new Department("Engineering");

    public static final LocalDate HIRE_DATE = LocalDate.of(2020, 3, 1);

    private Fixtures() {
    }

    public static Employee employeeIn(Country country) {
        return new Employee(
                "ACME-00001", "Ada", "Lovelace", "ada.lovelace@acme.co",
                ENGINEERING, country, "Senior Engineer", JobLevel.L3, Gender.FEMALE,
                null, HIRE_DATE, EmploymentStatus.ACTIVE);
    }

    public static Employee germanEmployee() {
        return employeeIn(GERMANY);
    }
}
