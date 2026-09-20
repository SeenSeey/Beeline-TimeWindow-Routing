package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.Skill;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Integer> {
}
