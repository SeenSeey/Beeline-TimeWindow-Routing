package stb.core.domain.repository.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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
            Path<Region> region = root.get(EngineerCrew_.REGION);

            predicates.add(
                    cb.or(
                            cb.equal(region.get(Region_.ID), crew.regionId()),
                            cb.equal(region.get(Region_.PARENT_REGION).get(Region_.ID), crew.regionId())
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
            Subquery<Integer> skillQuery = query.subquery(Integer.class);
            Root<EngineerCrew> crewRoot = skillQuery.correlate(root);
            skillQuery.select(cb.literal(1))
                    .where(cb.equal(crewRoot.join(EngineerCrew_.SKILLS).get(Skill_.ID), crew.skillId()));
            predicates.add(cb.exists(skillQuery));
        }

        return cb.and(predicates);
    }
}
