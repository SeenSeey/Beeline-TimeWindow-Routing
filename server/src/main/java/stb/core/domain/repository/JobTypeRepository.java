package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.JobType;

@Repository
public interface JobTypeRepository extends JpaRepository<JobType, Integer> {
}
