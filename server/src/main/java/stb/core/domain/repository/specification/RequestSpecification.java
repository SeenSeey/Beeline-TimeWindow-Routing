package stb.core.domain.repository.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import stb.core.domain.dto.RequestSearchDto;
import stb.core.domain.model.*;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class RequestSpecification implements Specification<Request> {

    private final RequestSearchDto request;

    @Override
    public @Nullable Predicate toPredicate(Root<Request> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        if (request.statusId() != null) {
            predicates.add(
                    cb.equal(
                            root.get(Request_.STATUS).get(RequestStatus_.ID),
                            request.statusId()
                    )
            );
        }

        if (request.urgent() != null) {
            predicates.add(
                    cb.equal(root.get(Request_.URGENT), request.urgent())
            );
        }

        if (request.regionId() != null) {
            predicates.add(
                    cb.equal(
                            root.get(Request_.REGION).get(Region_.ID),
                            request.regionId())
            );
        }

        if (request.jobTypeId() != null) {
            predicates.add(
                    cb.equal(
                            root.get(Request_.JOB_TYPE).get(JobType_.ID),
                            request.jobTypeId()
                    )
            );
        }

        if (request.noAssignedCrew() != null) {
            if (request.noAssignedCrew()) {
                predicates.add(
                        cb.isNull(root.get(Request_.ASSIGNED_CREW))
                );
            } else {
                predicates.add(
                        cb.isNotNull(root.get(Request_.ASSIGNED_CREW))
                );
            }
        }

        if (request.assignedCrewIds() != null
                && !request.assignedCrewIds().isEmpty()) {
            predicates.add(
                    root.get(Request_.ASSIGNED_CREW)
                            .get(EngineerCrew_.ID)
                            .in(request.assignedCrewIds())
            );
        }

        return cb.and(predicates);
    }
}
