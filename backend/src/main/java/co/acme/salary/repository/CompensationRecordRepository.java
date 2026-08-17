package co.acme.salary.repository;

import co.acme.salary.domain.CompensationRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompensationRecordRepository extends JpaRepository<CompensationRecord, Long> {

    List<CompensationRecord> findByEmployeeIdOrderByEffectiveFromAsc(Long employeeId);

    /** The salary currently in force, i.e. the one open record. */
    Optional<CompensationRecord> findByEmployeeIdAndEffectiveToIsNull(Long employeeId);
}
