package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.ScheduleSlot;
import stb.core.domain.repository.projection.CrewLatestSlot;
import stb.core.domain.repository.projection.EngineerCrewSlotsCount;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Integer> {

    @Query("""
            SELECT s FROM ScheduleSlot s
            JOIN FETCH s.schedule sc
            WHERE s.request.id = :requestId
            AND s.type = :type
            ORDER BY sc.targetDate DESC, sc.createdAt DESC
            """)
    List<ScheduleSlot> findAllByRequestIdAndType(@Param("requestId") int requestId,
                                                 @Param("type") ScheduleSlot.SlotType type);

    @Query("""
            SELECT s FROM ScheduleSlot s
            JOIN FETCH s.schedule sc
            JOIN FETCH s.request r
            WHERE sc.targetDate = :targetDate
            AND r.assignedCrew.id = :crewId
            AND s.type = :type
            AND NOT EXISTS (
                SELECT nextSchedule FROM Schedule nextSchedule
                WHERE nextSchedule.previousSchedule.id = sc.id
            )
            ORDER BY s.startTimeWindow
            """)
    List<ScheduleSlot> findAllByTargetDateAndCrewIdAndType(@Param("targetDate") LocalDate targetDate,
                                                           @Param("crewId") int crewId,
                                                           @Param("type") ScheduleSlot.SlotType type);

    @Query("""
            SELECT s FROM ScheduleSlot s
            WHERE s.request.assignedCrew.id = :crewId
            AND s.schedule.targetDate = :targetDate
            ORDER BY s.startTimeWindow
            """)
    List<ScheduleSlot> findAllIncomingSlotsByTargetDateAndCrewId(LocalDate targetDate, int crewId);

    @Query(value = """
            SELECT slot.* FROM schedule_slots slot
            JOIN schedules schedule ON slot.schedule_id = schedule.id
            JOIN requests request ON request.id = slot.request_id
            WHERE request.id = :requestId
            AND request.status_id IN (SELECT id FROM request_statuses WHERE keyword = 'CANCELED')
            AND NOT EXISTS (
                SELECT 1 FROM schedules sc
                WHERE sc.previous_schedule_id = schedule.id
            )
            LIMIT 1
            """, nativeQuery = true)
    Optional<ScheduleSlot> findCanceledSlotByRequestId(int requestId);

    @Query("""
            SELECT
                s.request.assignedCrew.id as engineerCrewId,
                count(s.id) as activeSlotsCount
            FROM ScheduleSlot s
            WHERE s.schedule.id = :scheduleId
            AND s.request.assignedCrew.id IN (:crewsIds)
            GROUP BY s.request.assignedCrew.id
            """)
    List<EngineerCrewSlotsCount> countAllByScheduleAndEngineerCrewIdIn(int scheduleId, List<Integer> crewsIds);

    @Query(value = """
            WITH slots AS (
                SELECT
                    ss.*,
                    r.assigned_crew_id,
                    r.latitude,
                    r.longitude,
                    r.region_id,
                    row_number() OVER (PARTITION BY r.assigned_crew_id ORDER BY ss.start_time_window DESC) rn FROM schedule_slots ss
                JOIN requests r ON r.id = ss.request_id
                JOIN request_statuses rs ON rs.id = r.status_id
                JOIN schedules s ON s.id = ss.schedule_id
                WHERE rs.keyword IN ('IN_PROGRESS', 'COMPLETED')
                AND NOT EXISTS (
                    SELECT 1 FROM schedules s2 WHERE s2.previous_schedule_id = s.id
                )
                ORDER BY ss.start_time_window
            ),
            latest_slots AS (
                SELECT * FROM slots WHERE rn = 1
            )
            SELECT
                ls.id "slotId",
                ls.request_id "requestId",
                ls.start_time_window "startTimeWindow",
                ls.end_time_window "endTimeWindow",
                ls.assigned_crew_id "crewId",
                ls.latitude,
                ls.longitude
            FROM latest_slots ls
            JOIN regions rg ON rg.id = ls.region_id
            WHERE (rg.id = :regionId OR rg.parent_region_id = :regionId)
            """, nativeQuery = true)
    List<CrewLatestSlot> findLatestSlotsForCrewsByRegionId(int regionId);

    @Query("""
            SELECT s FROM ScheduleSlot s
            WHERE s.request.id = :requestId
            AND NOT EXISTS (
                SELECT 1 FROM Schedule sc
                WHERE sc.previousSchedule.id = s.id
            )
            ORDER BY s.id DESC
            LIMIT 1
            """)
    ScheduleSlot findActiveSlotByRequestId(int requestId);
}
