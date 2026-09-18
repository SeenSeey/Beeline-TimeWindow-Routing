package stb.core.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "job_types")
@Getter
@Setter(AccessLevel.PROTECTED)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobType extends BaseEntity {

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "max_travel_time_span_minutes", nullable = false)
    private int maxTravelTimeSpanMinutes;

    @Column(name = "job_time_span_minutes", nullable = false)
    private int jobTimeSpanMinutes;

    @Column(name = "total_time_span_minutes", nullable = false)
    private int totalTimeSpanMinutes;

    @Column(name = "keyword", nullable = false)
    @Enumerated(EnumType.STRING)
    private Keyword keyword;

    public enum Keyword {

        LOCAL_WORK,
        INSTALLATION,
        ADDON,
        EMERGENCY
    }
}
