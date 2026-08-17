package co.acme.salary.repository;

import co.acme.salary.domain.CompensationRecord;
import co.acme.salary.domain.Country;
import co.acme.salary.domain.Department;
import co.acme.salary.domain.Employee;
import co.acme.salary.query.EmployeeFilter;
import co.acme.salary.query.EmployeeSortKey;
import co.acme.salary.query.EmployeeSummary;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Hand-written criteria query for the directory.
 *
 * <p>Written by hand rather than derived from a method name because this one query has to do four
 * things at once — filter on six optional dimensions, join to the employee's <em>open</em>
 * compensation record, sort by any of seven columns including salary, and project straight into a
 * read model. Spring Data can express any one of those; expressing all four together is clearer
 * as explicit criteria code than as a pile of specifications.
 */
public class EmployeeSearchRepositoryImpl implements EmployeeSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<EmployeeSummary> search(EmployeeFilter filter, EmployeeSortKey sortKey,
                                        Sort.Direction direction, Pageable pageable) {
        long total = count(filter);
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        return new PageImpl<>(fetchPage(filter, sortKey, direction, pageable), pageable, total);
    }

    private List<EmployeeSummary> fetchPage(EmployeeFilter filter, EmployeeSortKey sortKey,
                                            Sort.Direction direction, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<EmployeeSummary> query = cb.createQuery(EmployeeSummary.class);
        Root<Employee> employee = query.from(Employee.class);
        Joins joins = Joins.of(cb, employee);

        query.select(cb.construct(EmployeeSummary.class,
                employee.get("id"),
                employee.get("employeeCode"),
                employee.get("firstName"),
                employee.get("lastName"),
                employee.get("email"),
                joins.department().get("id"),
                joins.department().get("name"),
                joins.country().get("code"),
                joins.country().get("name"),
                employee.get("jobTitle"),
                employee.get("level"),
                employee.get("status"),
                employee.get("hireDate"),
                joins.currentSalary().get("amount"),
                joins.currentSalary().get("currencyCode"),
                joins.currentSalary().get("annualUsdAmount")));

        query.where(predicates(cb, employee, joins, filter));
        query.orderBy(orderBy(cb, employee, joins, sortKey, direction));

        return entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
    }

    private long count(EmployeeFilter filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Employee> employee = query.from(Employee.class);
        Joins joins = Joins.of(cb, employee);

        query.select(cb.count(employee));
        query.where(predicates(cb, employee, joins, filter));

        return entityManager.createQuery(query).getSingleResult();
    }

    private Predicate[] predicates(CriteriaBuilder cb, Root<Employee> employee, Joins joins,
                                   EmployeeFilter filter) {
        List<Predicate> predicates = new ArrayList<>();

        if (filter.hasSearchTerm()) {
            String pattern = filter.searchPattern();
            Expression<String> fullName = cb.lower(
                    cb.concat(cb.concat(employee.get("firstName"), " "), employee.get("lastName")));
            predicates.add(cb.or(
                    cb.like(fullName, pattern),
                    cb.like(cb.lower(employee.get("email")), pattern),
                    cb.like(cb.lower(employee.get("employeeCode")), pattern)));
        }
        if (filter.departmentId() != null) {
            predicates.add(cb.equal(joins.department().get("id"), filter.departmentId()));
        }
        if (filter.countryCode() != null) {
            predicates.add(cb.equal(joins.country().get("code"), filter.countryCode()));
        }
        if (filter.level() != null) {
            predicates.add(cb.equal(employee.get("level"), filter.level()));
        }
        if (filter.status() != null) {
            predicates.add(cb.equal(employee.get("status"), filter.status()));
        }
        if (filter.minAnnualUsd() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                    joins.currentSalary().get("annualUsdAmount"), filter.minAnnualUsd()));
        }
        if (filter.maxAnnualUsd() != null) {
            predicates.add(cb.lessThanOrEqualTo(
                    joins.currentSalary().get("annualUsdAmount"), filter.maxAnnualUsd()));
        }
        return predicates.toArray(new Predicate[0]);
    }

    private List<Order> orderBy(CriteriaBuilder cb, Root<Employee> employee, Joins joins,
                                EmployeeSortKey sortKey, Sort.Direction direction) {
        List<Expression<?>> columns = switch (sortKey) {
            case NAME -> List.of(employee.get("lastName"), employee.get("firstName"));
            case EMPLOYEE_CODE -> List.of(employee.get("employeeCode"));
            case SALARY -> List.of(joins.currentSalary().get("annualUsdAmount"));
            case DEPARTMENT -> List.of(joins.department().get("name"));
            case COUNTRY -> List.of(joins.country().get("name"));
            case LEVEL -> List.of(employee.get("level"));
            case HIRE_DATE -> List.of(employee.get("hireDate"));
        };

        List<Order> orders = new ArrayList<>(columns.stream()
                .map(column -> direction.isAscending() ? cb.asc(column) : cb.desc(column))
                .toList());
        // A stable tie-break, so paging through equal salaries never repeats or skips a row.
        orders.add(cb.asc(employee.get("id")));
        return orders;
    }

    /**
     * The three joins every variant of this query needs. The compensation join is a left join
     * narrowed to the open record, which is what makes "current salary" a column rather than a
     * second query.
     */
    private record Joins(Join<Employee, Department> department,
                         Join<Employee, Country> country,
                         Join<Employee, CompensationRecord> currentSalary) {

        static Joins of(CriteriaBuilder cb, Root<Employee> employee) {
            Join<Employee, CompensationRecord> salary =
                    employee.join("compensationRecords", JoinType.LEFT);
            salary.on(cb.isNull(salary.get("effectiveTo")));
            return new Joins(
                    employee.join("department", JoinType.INNER),
                    employee.join("country", JoinType.INNER),
                    salary);
        }
    }
}
