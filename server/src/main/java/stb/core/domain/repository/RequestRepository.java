package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.Request;
import stb.core.domain.model.RequestStatus;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Integer>, JpaSpecificationExecutor<Request> {

    @Query(value = "SELECT setval('seq_requests', (SELECT MAX(id) FROM requests))", nativeQuery = true)
    long syncIdSequence();

    @Query("""
            SELECT r FROM Request r
            WHERE (r.region.id = :regionId OR r.region.parentRegion.id = :regionId)
            AND r.assignedCrew IS NULL
            AND r.completedAt IS NULL
            AND r.canceledAt IS NULL
            """)
    List<Request> findAllForSchedulingByRegionId(int regionId);

    @Query("""
            SELECT r FROM Request r
            WHERE (r.region.id = :regionId OR r.region.parentRegion.id = :regionId)
            AND r.completedAt IS NULL
            AND r.canceledAt IS NULL
            ORDER BY r.urgent DESC, r.deadline ASC
            """)
    List<Request> findAllActiveByRegionId(@Param("regionId") int regionId);

    @Query("""
            SELECT r FROM Request r
            WHERE (r.region.id = :regionId OR r.region.parentRegion.id = :regionId)
            AND r.assignedCrew IS NULL
            AND r.status.keyword = :status
            ORDER BY r.urgent DESC, r.deadline ASC
            """)
    List<Request> findAllUnassignedByRegionIdAndStatus(@Param("regionId") int regionId,
                                                       @Param("status") RequestStatus.Keyword status);

    @Query("""
            SELECT r FROM Request r
            WHERE (r.region.id = :regionId OR r.region.parentRegion.id = :regionId)
            ORDER BY r.id
            """)
    List<Request> findAllByRegionId(@Param("regionId") int regionId);

    @Query("""
            SELECT r FROM Request r
            WHERE r.assignedCrew.id = :crewId
            ORDER BY r.assignedAt DESC, r.id DESC
            """)
    List<Request> findAllByAssignedCrewId(@Param("crewId") int crewId);

    @Query("""
            SELECT r FROM Request r
            WHERE (r.region.id = :regionId OR r.region.parentRegion.id = :regionId)
            AND r.status.keyword IN ('IDLE', 'ASSIGNED')
            """)
    List<Request> findAllForRescheduleByRegionId(int regionId);

    @Query("""
            SELECT r FROM Request r
            WHERE r.assignedCrew.id = :assignedCrewId
            AND r.status.keyword = 'ASSIGNED'
            """)
    List<Request> findAllIncomingByAssignedCrewId(int assignedCrewId);
}
