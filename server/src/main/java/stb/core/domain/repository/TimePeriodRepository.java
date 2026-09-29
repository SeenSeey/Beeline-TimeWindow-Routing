package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.TimePeriod;

@Repository
public interface TimePeriodRepository extends JpaRepository<TimePeriod, Integer> {
}