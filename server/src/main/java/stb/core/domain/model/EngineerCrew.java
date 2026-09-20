package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "engineer_crews")
@Getter
@Setter(AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EngineerCrew extends BaseEntity {

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

    // TODO: пересмотреть логику связи JobType с бригадами. У бригад есть НАВЫКИ (например, установка роутера,
    //  подключение интернета и другие). НАВЫКИ должны соотноситься с типами работ для вычисления, может ли
    //  бригада выполнить данную заявку. Хранить тип работ с бригаде - неверно!

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "engineer_crew_skills",
            joinColumns = @JoinColumn(name = "crew_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private List<Skill> skills;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "engineer_crew_equipment",
            joinColumns = @JoinColumn(name = "crew_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_id"))
    private List<Equipment> equipment;
}
