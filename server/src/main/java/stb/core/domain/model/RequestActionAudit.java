package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_actions_audit")
@Getter
@Setter(AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequestActionAudit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(name = "action", nullable = false)
    @Enumerated(EnumType.STRING)
    private Action action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_crew_id", nullable = false)
    private EngineerCrew assignedCrew;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public enum Action {

        INITIAL_ASSIGNATION,
        ASSIGNED_CREW_CHANGE,
        REMOVE_FROM_SCHEDULE,
        COMPLETE_REQUEST,
        CANCEL_REQUEST
    }
}
