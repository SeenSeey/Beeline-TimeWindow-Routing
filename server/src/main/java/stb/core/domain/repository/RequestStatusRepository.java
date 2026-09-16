package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.RequestStatus;

@Repository
public interface RequestStatusRepository extends JpaRepository<RequestStatus, Integer> {
}
