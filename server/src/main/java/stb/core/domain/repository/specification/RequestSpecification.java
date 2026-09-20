package stb.core.domain.repository.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import stb.core.domain.dto.RequestSearchDto;
import stb.core.domain.model.Request;

import java.util.ArrayList;
import java.util.List;

public class RequestSpecification implements Specification<Request> {

    private final RequestSearchDto request;

    public RequestSpecification(RequestSearchDto request) {
        this.request = request;
    }

    @Override
    public @Nullable Predicate toPredicate(Root<Request> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        if (request.statusId() != null) {
            predicates.add(
                    cb.equal(
                            root.get("status").get("id"),
                            request.statusId()
                    )
            );
        }

        if (request.urgent() != null) {
            predicates.add(
                    cb.equal(root.get("urgent"), request.urgent())
            );
        }

        if (request.regionId() != null) {
            predicates.add(
                    cb.equal(
                            root.get("region").get("id"),
                            request.regionId())
            );
        }

        if (request.jobTypeId() != null) {
            predicates.add(
                    cb.equal(
                            root.get("jobType").get("id"),
                            request.jobTypeId()
                    )
            );
        }

        if (request.hasAssignedCrew() != null) {
            if (request.hasAssignedCrew()) {
                predicates.add(
                        cb.isNotNull(root.get("assignedCrew"))
                );
            } else {
                predicates.add(
                        cb.isNull(root.get("assignedCrew"))
                );
            }
        }

        if (request.assignedCrewIds() != null
                && !request.assignedCrewIds().isEmpty()) {
            predicates.add(
                    root.get("assignedCrew")
                            .get("id")
                            .in(request.assignedCrewIds())
            );
        }

        return cb.and(predicates);
    }
}
