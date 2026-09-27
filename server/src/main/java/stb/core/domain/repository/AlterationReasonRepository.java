package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.AlterationReason;

import java.util.List;

@Repository
public interface AlterationReasonRepository extends JpaRepository<AlterationReason, Integer> {

    @Query("""
            SELECT DISTINCT reason FROM AlterationReason reason
            JOIN FETCH reason.request request
            JOIN FETCH request.jobType jobType
            LEFT JOIN FETCH jobType.requiredSkills
            LEFT JOIN FETCH request.requiredVehicle
            LEFT JOIN FETCH request.requiredEquipment
            LEFT JOIN FETCH reason.causeRequest
            LEFT JOIN FETCH reason.causeCrew
            WHERE reason.schedule.id = :scheduleId
            ORDER BY reason.id
            """)
    List<AlterationReason> findAllByScheduleId(int scheduleId);
}
