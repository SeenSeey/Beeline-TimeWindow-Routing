package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "request_actions_audit", indexes = @Index(
        name = "idx_request_actions_audit_timeline",
        columnList = "request_id, timestamp DESC, id DESC"
))
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
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @Column(name = "action", nullable = false)
    @Enumerated(EnumType.STRING)
    private Action action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_crew_id")
    private EngineerCrew assignedCrew;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public enum Action {

        REQUEST_CREATED,
        REQUEST_UPDATED,
        INITIAL_ASSIGNMENT,
        ASSIGNED_CREW_CHANGE,
        REMOVE_FROM_SCHEDULE,
        SCHEDULE_CHANGE,
        COMPLETE_REQUEST,
        CANCEL_REQUEST
    }
}
