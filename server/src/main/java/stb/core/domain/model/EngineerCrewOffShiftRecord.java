package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "engineer_crew_off_shift_records", uniqueConstraints = @UniqueConstraint(columnNames = { "engineerCrew", "offShiftDate" }))
@Getter
@Setter(AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EngineerCrewOffShiftRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engineer_crew_id", nullable = false)
    private EngineerCrew engineerCrew;

    @Column(name = "off_shift_date", nullable = false)
    private LocalDate offShiftDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public EngineerCrewOffShiftRecord(EngineerCrew engineerCrew, LocalDate offShiftDate) {
        setEngineerCrew(engineerCrew);
        setOffShiftDate(offShiftDate);
        setCreatedAt(LocalDateTime.now());
    }
}
