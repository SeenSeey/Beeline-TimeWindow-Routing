package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.ScheduleSlot;

import java.util.List;

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
}
