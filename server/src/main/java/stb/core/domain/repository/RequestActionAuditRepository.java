package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.RequestActionAudit;

import java.util.List;

@Repository
public interface RequestActionAuditRepository extends JpaRepository<RequestActionAudit, Integer> {

    List<RequestActionAudit> findAllByRequestIdOrderByOccurredAtDescIdDesc(int requestId);
}
