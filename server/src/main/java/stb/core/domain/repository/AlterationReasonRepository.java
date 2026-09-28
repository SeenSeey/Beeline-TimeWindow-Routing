package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.AlterationReason;

import java.util.Collection;
import java.util.List;

@Repository
public interface AlterationReasonRepository extends JpaRepository<AlterationReason, Integer> {

    List<AlterationReason> findAllDistinctByScheduleIdOrderById(int scheduleId);

    void deleteAllByScheduleIdAndRequestIdIn(int scheduleId, Collection<Integer> requestIds);
}
