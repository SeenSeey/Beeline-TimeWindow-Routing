package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "request_actions_audit", indexes = @Index(
        name = "idx_request_actions_audit_timeline",
        columnList = "request_id, timestamp DESC, id DESC"
))
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequestActionAudit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @Column(name = "action", nullable = false)
    @Enumerated(EnumType.STRING)
    private Action action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_crew_id")
    private EngineerCrew assignedCrew;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "planned_start")
    private LocalTime plannedStart;

    @Column(name = "planned_end")
    private LocalTime plannedEnd;

    public static RequestActionAudit requestCreated(Request request, LocalDateTime timestamp) {
        return action(request, Action.REQUEST_CREATED, timestamp);
    }

    public static RequestActionAudit assigned(Request request,
                                              EngineerCrew assignedCrew,
                                              LocalTime plannedStart,
                                              LocalTime plannedEnd,
                                              LocalDateTime timestamp) {
        return create(request, Action.ASSIGNED, Objects.requireNonNull(assignedCrew, "assignedCrew не должна быть null"),
                plannedStart, plannedEnd, timestamp);
    }

    public static RequestActionAudit rescheduled(Request request,
                                                 EngineerCrew assignedCrew,
                                                 LocalTime plannedStart,
                                                 LocalTime plannedEnd,
                                                 LocalDateTime timestamp) {
        return create(request, Action.RESCHEDULED, assignedCrew, plannedStart, plannedEnd, timestamp);
    }

    public static RequestActionAudit removedFromSchedule(Request request, LocalDateTime timestamp) {
        return action(request, Action.REMOVED_FROM_SCHEDULE, timestamp);
    }

    public static RequestActionAudit started(Request request, LocalDateTime timestamp) {
        return action(request, Action.STARTED, timestamp);
    }

    public static RequestActionAudit completed(Request request, LocalDateTime timestamp) {
        return action(request, Action.COMPLETED, timestamp);
    }

    public static RequestActionAudit canceled(Request request, LocalDateTime timestamp) {
        return action(request, Action.CANCELED, timestamp);
    }

    private static RequestActionAudit action(Request request, Action action, LocalDateTime timestamp) {
        return create(request, action, null, null, null, timestamp);
    }

    private static RequestActionAudit create(Request request,
                                             Action action,
                                             EngineerCrew assignedCrew,
                                             LocalTime plannedStart,
                                             LocalTime plannedEnd,
                                             LocalDateTime timestamp) {
        Objects.requireNonNull(request, "request не должна быть null");
        Objects.requireNonNull(action, "action не должен быть null");
        Objects.requireNonNull(timestamp, "timestamp не должен быть null");
        if (action == Action.ASSIGNED || action == Action.RESCHEDULED) {
            Objects.requireNonNull(plannedStart, "plannedStart не должно быть null");
            Objects.requireNonNull(plannedEnd, "plannedEnd не должно быть null");
        }
        RequestActionAudit audit = new RequestActionAudit();
        audit.request = request;
        audit.action = action;
        audit.assignedCrew = assignedCrew;
        audit.plannedStart = plannedStart;
        audit.plannedEnd = plannedEnd;
        audit.timestamp = timestamp;
        return audit;
    }

    public enum Action {

        REQUEST_CREATED("Заявка создана"),
        ASSIGNED("Назначена бригаде"),
        RESCHEDULED("Перенесена"),
        REMOVED_FROM_SCHEDULE("Снята с расписания"),
        STARTED("Взята в работу"),
        COMPLETED("Выполнена"),
        CANCELED("Отменена");

        @Getter
        private final String displayName;

        Action(String displayName) {
            this.displayName = displayName;
        }
    }
}
