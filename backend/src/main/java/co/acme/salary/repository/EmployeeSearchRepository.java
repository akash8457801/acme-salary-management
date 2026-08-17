package co.acme.salary.repository;

import co.acme.salary.query.EmployeeFilter;
import co.acme.salary.query.EmployeeSortKey;
import co.acme.salary.query.EmployeeSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** The directory query: filter, sort and page ten thousand employees inside the database. */
public interface EmployeeSearchRepository {

    Page<EmployeeSummary> search(EmployeeFilter filter, EmployeeSortKey sortKey,
                                 Sort.Direction direction, Pageable pageable);
}
