package co.acme.salary.repository;

import co.acme.salary.domain.Employee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, EmployeeSearchRepository {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    /** Names for a set of manager ids, so a list of employees can show who they report to. */
    @Query("select e from Employee e where e.id in :ids")
    List<Employee> findAllByIdIn(List<Long> ids);

    /**
     * The highest employee code issued so far. Codes are fixed-width and zero-padded, so the
     * lexical maximum is also the numeric one.
     */
    @Query("select max(e.employeeCode) from Employee e")
    Optional<String> findHighestEmployeeCode();
}
