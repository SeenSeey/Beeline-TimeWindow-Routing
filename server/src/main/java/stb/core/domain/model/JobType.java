package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "job_types")
@Getter
@Setter(AccessLevel.PROTECTED)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobType extends BaseEntity {

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "job_type_required_skills",
            joinColumns = @JoinColumn(name = "job_type_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private List<Skill> requiredSkills;

    @Column(name = "max_travel_time_span_minutes", nullable = false)
    private int maxTravelTimeSpanMinutes;

    @Column(name = "job_time_span_minutes", nullable = false)
    private int jobTimeSpanMinutes;

    @Column(name = "total_time_span_minutes", nullable = false)
    private int totalTimeSpanMinutes;

    @Column(name = "keyword", nullable = false)
    @Enumerated(EnumType.STRING)
    private Keyword keyword;

    @Column(name = "deadline_in_days", nullable = false)
    private int deadlineInDays;

    public enum Keyword {

        LOCAL_WORK,
        INSTALLATION,
        ADDON,
        EMERGENCY
    }
}
