package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.Equipment;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Integer> {
}
