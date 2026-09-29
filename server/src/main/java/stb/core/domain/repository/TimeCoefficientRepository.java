package stb.core.domain.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.TimeCoefficient;
import stb.core.domain.repository.projection.SpeedIntervalProjection;

import java.util.List;

@Repository
public interface TimeCoefficientRepository extends JpaRepository<TimeCoefficient, Integer> {

    @Query("""
           select
                p.startTime startTime,
                p.endTime endTime,
                cast(s.coefficient * c.coefficient as Double) coefficient
           from TimeCoefficient c
           join c.timePeriod p,
                SeasonMonth sm
           join sm.season s
           where sm.month = :month
             and c.dayOfWeek = :dayOfWeek
           order by p.startTime
           """)
    List<SpeedIntervalProjection> findIntervals(@Param("month") int month,
                                                @Param("dayOfWeek") int dayOfWeek);
}
