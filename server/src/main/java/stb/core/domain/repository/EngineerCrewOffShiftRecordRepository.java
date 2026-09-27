package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.EngineerCrewOffShiftRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EngineerCrewOffShiftRecordRepository extends JpaRepository<EngineerCrewOffShiftRecord, Integer> {

    Optional<EngineerCrewOffShiftRecord> findByEngineerCrewIdAndOffShiftDate(int engineerCrewId, LocalDate offShiftDate);

    List<EngineerCrewOffShiftRecord> findAllByEngineerCrewRegionIdAndOffShiftDate(int regionId, LocalDate offShiftDate);
}
