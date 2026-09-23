package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.Request;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Integer>, JpaSpecificationExecutor<Request> {

    @Query("""
            SELECT r FROM Request r
            WHERE r.region.id = :regionId
            AND r.assignedCrew IS NULL
            AND r.completedAt IS NULL
            AND r.canceledAt IS NULL
            """)
    List<Request> findAllForSchedulingByRegionId(int regionId);

    @Query("""
            SELECT r FROM Request r
            WHERE r.region.id = :regionId
            AND r.completedAt IS NULL
            AND r.canceledAt IS NULL
            ORDER BY r.urgent DESC, r.deadline ASC
            """)
    List<Request> findAllActiveByRegionId(@Param("regionId") int regionId);

    @Query("""
            SELECT r FROM Request r
            WHERE r.region.id = :regionId
            ORDER BY r.id
            """)
    List<Request> findAllByRegionId(@Param("regionId") int regionId);
}
