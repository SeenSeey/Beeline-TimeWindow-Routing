package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.Region;

@Repository
public interface RegionRepository extends JpaRepository<Region, Integer> {
}
