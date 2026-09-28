package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "request_actions_audit")
@Getter
@Setter(AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
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

    public static RequestActionAudit requestCreated(Request request) {
        return action(request, Action.REQUEST_CREATED);
    }

    public static RequestActionAudit assigned(Request request,
                                              EngineerCrew assignedCrew,
                                              LocalTime plannedStart,
                                              LocalTime plannedEnd) {

        return create(
                request,
                Action.ASSIGNED,
                Objects.requireNonNull(assignedCrew, "assignedCrew не должна быть null"),
                plannedStart,
                plannedEnd
        );
    }

    public static RequestActionAudit reassigned(Request request,
                                                EngineerCrew assignedCrew,
                                                LocalTime plannedStart,
                                                LocalTime plannedEnd) {

        return create(
                request,
                Action.REASSIGNED,
                Objects.requireNonNull(assignedCrew, "assignedCrew не должна быть null"),
                plannedStart,
                plannedEnd
        );
    }

    public static RequestActionAudit rescheduled(Request request,
                                                 LocalTime plannedStart,
                                                 LocalTime plannedEnd) {

        return create(request, Action.RESCHEDULED, null, plannedStart, plannedEnd);
    }

    public static RequestActionAudit removedFromSchedule(Request request) {
        return action(request, Action.REMOVED_FROM_SCHEDULE);
    }

    public static RequestActionAudit started(Request request) {
        return action(request, Action.STARTED);
    }

    public static RequestActionAudit completed(Request request) {
        return action(request, Action.COMPLETED);
    }

    public static RequestActionAudit canceled(Request request) {
        return action(request, Action.CANCELED);
    }

    private static RequestActionAudit action(Request request, Action action) {
        return create(request, action, null, null, null);
    }

    private static RequestActionAudit create(Request request,
                                             Action action,
                                             EngineerCrew assignedCrew,
                                             LocalTime plannedStart,
                                             LocalTime plannedEnd) {

        return RequestActionAudit.builder()
                .request(request)
                .action(action)
                .assignedCrew(assignedCrew)
                .plannedStart(plannedStart)
                .plannedEnd(plannedEnd)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public enum Action {

        REQUEST_CREATED("Заявка создана"),
        ASSIGNED("Назначена бригаде"),
        REASSIGNED("Бригада изменена"),
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
