package stb.core.domain.repository;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import stb.core.domain.model.RequestActionAudit;
import stb.core.domain.model.RequestStatus;

@Repository
@RequiredArgsConstructor
public class DevStateResetRepository {

    private final EntityManager entityManager;

    public void clearAll() {
        lockTables();

        entityManager.createQuery("delete from RequestActionAudit").executeUpdate();
        entityManager.createQuery("delete from EngineerCrewEquipment").executeUpdate();
        entityManager.createQuery("delete from AlterationReason").executeUpdate();
        entityManager.createQuery("delete from ScheduleSlot").executeUpdate();
        entityManager.createQuery("delete from Schedule").executeUpdate();
        entityManager.createQuery("delete from EngineerCrewOffShiftRecord").executeUpdate();
        entityManager.createQuery("delete from Request").executeUpdate();
        entityManager.createQuery("delete from Equipment").executeUpdate();
        entityManager.createQuery("delete from JobType").executeUpdate();
        entityManager.createQuery("delete from EngineerCrew").executeUpdate();
        entityManager.createQuery("delete from Region").executeUpdate();
        entityManager.createQuery("delete from RequestStatus").executeUpdate();
        entityManager.createQuery("delete from Skill").executeUpdate();
        entityManager.createQuery("delete from Vehicle").executeUpdate();
        entityManager.createQuery("delete from SeasonMonth").executeUpdate();
        entityManager.createQuery("delete from Season").executeUpdate();
        entityManager.createQuery("delete from TimeCoefficient").executeUpdate();
        entityManager.createQuery("delete from TimePeriod").executeUpdate();

        entityManager.clear();
    }

    public void reset() {
        lockTables();

        RequestStatus idleStatus = entityManager.createQuery("""
                select status
                from RequestStatus status
                where status.keyword = :keyword
                """, RequestStatus.class)
                .setParameter("keyword", RequestStatus.Keyword.IDLE)
                .getSingleResult();

        entityManager.createQuery("delete from RequestActionAudit audit where audit.action <> :created")
                .setParameter("created", RequestActionAudit.Action.REQUEST_CREATED)
                .executeUpdate();
        entityManager.createQuery("delete from EngineerCrewEquipment").executeUpdate();
        entityManager.createQuery("delete from AlterationReason").executeUpdate();
        entityManager.createQuery("delete from ScheduleSlot").executeUpdate();
        entityManager.createQuery("delete from Schedule").executeUpdate();
        entityManager.createQuery("delete from EngineerCrewOffShiftRecord").executeUpdate();
        entityManager.createQuery("""
                update Request request
                set request.status = :status,
                    request.assignedCrew = null,
                    request.assignedAt = null,
                    request.arrivedAt = null,
                    request.completedAt = null,
                    request.canceledAt = null
                """)
                .setParameter("status", idleStatus)
                .executeUpdate();

        entityManager.clear();
    }

    private void lockTables() {
        entityManager.createNativeQuery("""
                lock table requests,
                    schedules,
                    schedule_slots,
                    alteration_reasons,
                    request_actions_audit,
                    engineer_crew_equipment,
                    engineer_crew_off_shift_records
                in share row exclusive mode
                """)
                .executeUpdate();
    }
}
