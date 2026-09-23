package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.EngineerCrew;

import java.util.List;

@Repository
public interface EngineerCrewRepository extends JpaRepository<EngineerCrew, Integer> , JpaSpecificationExecutor<EngineerCrew> {

    List<EngineerCrew> findAllByRegionId(int regionId);
}
