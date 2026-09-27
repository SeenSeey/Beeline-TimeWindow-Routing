package stb.core.domain.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.Schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {

    @Query("""
            SELECT count(s.id) FROM Schedule s
            WHERE s.targetDate = :targetDate
            AND NOT EXISTS (
                SELECT s1 FROM Schedule s1
                WHERE s1.previousSchedule.id = s.id
            )
            """)
    int countAllByTargetDate(LocalDate targetDate);

    @Query("""
            DELETE FROM Schedule s
            WHERE s.status = :status
            """)
    @Modifying
    long deleteAllByStatus(Schedule.Status status);

    List<Schedule> findAllByTargetDateAndStatus(LocalDate targetDate, Schedule.Status status);

    @Query("""
            SELECT s FROM Schedule s
            WHERE s.region.id = :regionId
            AND s.targetDate = :targetDate
            AND NOT EXISTS (
                SELECT s1 FROM Schedule s1
                WHERE s1.previousSchedule.id = s.id
            )
            """)
    @EntityGraph(attributePaths = {
            "region",
            "slots",
            "slots.request",
            "slots.request.jobType",
            "slots.request.requiredVehicle",
            "slots.request.assignedCrew",
            "slots.engineerCrew"
    })
    Optional<Schedule> findByTargetDateAndRegionId(LocalDate targetDate, int regionId);
}
