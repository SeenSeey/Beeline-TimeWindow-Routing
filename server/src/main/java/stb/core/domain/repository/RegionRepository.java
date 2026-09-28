package stb.core.domain.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.Region;

import java.util.List;

@Repository
public interface RegionRepository extends JpaRepository<Region, Integer> {

    List<Region> findAllByParentRegionIsNull(Sort sort);

    List<Region> findAllByParentRegionId(int parentRegionId);
}
