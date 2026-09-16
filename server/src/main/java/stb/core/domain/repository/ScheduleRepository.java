package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.Schedule;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {
}
