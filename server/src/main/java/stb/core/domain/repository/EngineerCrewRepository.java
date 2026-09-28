package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.EngineerCrew;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EngineerCrewRepository extends JpaRepository<EngineerCrew, Integer> , JpaSpecificationExecutor<EngineerCrew> {

    @Query("""
            SELECT c FROM EngineerCrew c
            WHERE (c.region.id = :regionId OR c.region.parentRegion.id = :regionId)
            AND NOT EXISTS (
                SELECT 1 FROM EngineerCrewOffShiftRecord r
                WHERE r.engineerCrew.id = c.id
                AND r.offShiftDate = :targetDate
            )
            """)
    List<EngineerCrew> findAllActiveByRegionIdAndTargetDate(int regionId, LocalDate targetDate);

    @Query("""
            SELECT c FROM EngineerCrew c
            WHERE (c.region.id = :regionId OR c.region.parentRegion.id = :regionId)
            """)
    List<EngineerCrew> findAllByRegionId(int regionId);
}
