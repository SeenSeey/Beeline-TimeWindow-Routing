package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;
import stb.core.domain.common.GeopositionProvider;
import stb.core.domain.exception.DomainException;

import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "engineer_crews")
@Getter
@Setter(AccessLevel.PROTECTED)
@AllArgsConstructor()
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EngineerCrew extends BaseEntity implements GeopositionProvider {

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "shift_start_time", nullable = false)
    private LocalTime shiftStartTime;

    @Column(name = "shift_end_time", nullable = false)
    private LocalTime shiftEndTime;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "engineer_crew_skills",
            joinColumns = @JoinColumn(name = "crew_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private List<Skill> skills;

    @OneToMany(mappedBy = "crew", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EngineerCrewEquipment> equipment;

    public void issueEquipment(List<Equipment> routeEquipment) {
        for (EngineerCrewEquipment item : equipment) {
            item.clear();
        }

        for (Equipment type : routeEquipment) {
            EngineerCrewEquipment item = findEquipment(type);

            if (item == null) {
                equipment.add(new EngineerCrewEquipment(this, type, 1));
            } else {
                item.add(1);
            }
        }
    }

    public void addEquipment(Equipment type, int quantity) {
        if (loadedEquipmentCount() + quantity > vehicle.getEquipmentCapacity()) {
            throw new DomainException.ConflictException("Транспорт \"%s\" вмещает не больше %d ед. оборудования".formatted(
                    vehicle.getDisplayName(), vehicle.getEquipmentCapacity()));
        }

        EngineerCrewEquipment item = findEquipment(type);

        if (item == null) {
            equipment.add(new EngineerCrewEquipment(this, type, quantity));
        } else {
            item.add(quantity);
        }
    }

    public void takeEquipment(Equipment type) {
        EngineerCrewEquipment item = findEquipment(type);

        if (item == null || item.getQuantity() == 0) {
            throw new DomainException.ConflictException("У бригады нет оборудования \"%s\"".formatted(type.getDisplayName()));
        }

        item.take();
    }

    public int loadedEquipmentCount() {
        return equipment.stream().mapToInt(EngineerCrewEquipment::getQuantity).sum();
    }

    private EngineerCrewEquipment findEquipment(Equipment type) {
        for (EngineerCrewEquipment item : equipment) {
            if (item.getEquipment().getId() == type.getId()) {
                return item;
            }
        }

        return null;
    }

    @Override
    public double getLatitude() {
        return region.getOfficeLatitude();
    }

    @Override
    public double getLongitude() {
        return region.getOfficeLongitude();
    }
}
