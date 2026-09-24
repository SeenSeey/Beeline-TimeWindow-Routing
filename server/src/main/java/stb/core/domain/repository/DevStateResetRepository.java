package stb.core.domain.repository;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import stb.core.domain.model.RequestStatus;

@Repository
@RequiredArgsConstructor
public class DevStateResetRepository {

    private final EntityManager entityManager;

    public void reset() {
        lockTables();

        RequestStatus idleStatus = entityManager.createQuery("""
                select status
                from RequestStatus status
                where status.keyword = :keyword
                """, RequestStatus.class)
                .setParameter("keyword", RequestStatus.Keyword.IDLE)
                .getSingleResult();

        entityManager.createQuery("delete from RequestActionAudit").executeUpdate();
        entityManager.createQuery("delete from ScheduleSlot").executeUpdate();
        entityManager.createQuery("delete from Schedule").executeUpdate();
        entityManager.createQuery("""
                update Request request
                set request.status = :status,
                    request.assignedCrew = null,
                    request.assignedAt = null,
                    request.completedAt = null,
                    request.canceledAt = null
                """)
                .setParameter("status", idleStatus)
                .executeUpdate();

        entityManager.clear();
    }

    private void lockTables() {
        entityManager.createNativeQuery("""
                lock table requests, schedules, schedule_slots, request_actions_audit
                in share row exclusive mode
                """)
                .executeUpdate();
    }
}
