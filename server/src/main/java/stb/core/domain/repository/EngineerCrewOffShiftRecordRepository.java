package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.EngineerCrewOffShiftRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EngineerCrewOffShiftRecordRepository extends JpaRepository<EngineerCrewOffShiftRecord, Integer> {

    Optional<EngineerCrewOffShiftRecord> findByEngineerCrewIdAndOffShiftDate(int engineerCrewId, LocalDate offShiftDate);

    @Query("""
            SELECT r FROM EngineerCrewOffShiftRecord r
            WHERE (r.engineerCrew.region.id = :regionId OR r.engineerCrew.region.parentRegion.id = :regionId)
            AND r.offShiftDate = :offShiftDate
            """)
    List<EngineerCrewOffShiftRecord> findAllByEngineerCrewRegionIdAndOffShiftDate(int regionId, LocalDate offShiftDate);
}
