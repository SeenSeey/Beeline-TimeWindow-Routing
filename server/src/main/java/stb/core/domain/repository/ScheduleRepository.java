package stb.core.domain.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.Schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {

    List<Schedule> findAllByTargetDate(LocalDate targetDate);
    int countAllByTargetDate(LocalDate targetDate);

    long deleteAllByStatus(Schedule.Status status);

    List<Schedule> findAllByTargetDateAndStatus(LocalDate targetDate, Schedule.Status status);

    @EntityGraph(attributePaths = {
            "region",
            "slots",
            "slots.request",
            "slots.request.jobType",
            "slots.request.requiredVehicle",
            "slots.request.assignedCrew"
    })
    Optional<Schedule> findByTargetDateAndRegionId(LocalDate targetDate, int regionId);
}
