package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "engineer_crew_equipment", uniqueConstraints = @UniqueConstraint(columnNames = { "crew_id", "equipment_id" }))
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EngineerCrewEquipment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private EngineerCrew crew;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    public EngineerCrewEquipment(EngineerCrew crew, Equipment equipment, int quantity) {
        setCrew(crew);
        setEquipment(equipment);
        setQuantity(quantity);
    }

    public void add(int quantity) {
        setQuantity(this.quantity + quantity);
    }

    public void take() {
        setQuantity(this.quantity - 1);
    }

    public void clear() {
        setQuantity(0);
    }
}
