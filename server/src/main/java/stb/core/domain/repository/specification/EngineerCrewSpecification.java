package stb.core.domain.repository.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import stb.core.domain.dto.CrewSearchDto;
import stb.core.domain.model.*;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class EngineerCrewSpecification implements Specification<EngineerCrew> {

    private final CrewSearchDto crew;

    @Override
    public @Nullable Predicate toPredicate(Root<EngineerCrew> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        if (crew.name() != null && !crew.name().isBlank()) {
            String pattern = "%" + crew.name().trim().toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get(EngineerCrew_.NAME)), pattern));
        }

        if (crew.regionId() != null) {
            predicates.add(
                    cb.equal(
                            root.get(EngineerCrew_.REGION).get(Region_.ID),
                            crew.regionId()
                    )
            );
        }

        if (crew.vehicleId() != null) {
            predicates.add(
                    cb.equal(
                            root.get(EngineerCrew_.VEHICLE).get(Vehicle_.ID),
                            crew.vehicleId()
                    )
            );
        }

        if (crew.skillId() != null) {
            predicates.add(
                    cb.equal(
                            root.join(EngineerCrew_.SKILLS).get(Skill_.ID),
                            crew.skillId()
                    )
            );
            query.distinct(true);
        }

        return cb.and(predicates);
    }
}
