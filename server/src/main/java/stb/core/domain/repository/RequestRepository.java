package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.Request;

@Repository
public interface RequestRepository
        extends JpaRepository<Request, Integer>,
                JpaSpecificationExecutor<Request> {
}
