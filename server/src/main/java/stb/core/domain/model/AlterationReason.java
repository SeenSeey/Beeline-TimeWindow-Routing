package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "alteration_reasons", uniqueConstraints = @UniqueConstraint(
        name = "uk_alteration_reasons_schedule_request",
        columnNames = {"schedule_id", "request_id"}
))
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlterationReason extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cause_request_id")
    private Request causeRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cause_crew_id")
    private EngineerCrew causeCrew;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_crew_id")
    private EngineerCrew previousCrew;

    @Column(name = "previous_start_time")
    private LocalTime previousStartTime;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static AlterationReason causedByRequest(Schedule schedule,
                                                   Request request,
                                                   Type type,
                                                   Request causeRequest,
                                                   LocalDateTime createdAt) {
        return new AlterationReason(schedule, request, type, Cause.REQUEST,
                Objects.requireNonNull(causeRequest, "causeRequest не должна быть null"), null, null, createdAt);
    }

    public static AlterationReason causedByCrew(Schedule schedule,
                                                Request request,
                                                Type type,
                                                EngineerCrew causeCrew,
                                                LocalDateTime createdAt) {
        return new AlterationReason(schedule, request, type, Cause.CREW, null,
                Objects.requireNonNull(causeCrew, "causeCrew не должна быть null"), null, createdAt);
    }

    public static AlterationReason unscheduled(Schedule schedule,
                                               Request request,
                                               Type type,
                                               LocalDateTime createdAt) {
        return new AlterationReason(schedule, request, type, Cause.NONE, null, null, null, createdAt);
    }

    public static AlterationReason movedByRequest(Schedule schedule,
                                                  ScheduleSlot previousSlot,
                                                  Type type,
                                                  Request causeRequest,
                                                  LocalDateTime createdAt) {
        return new AlterationReason(schedule, previousSlot.getRequest(), type, Cause.REQUEST,
                Objects.requireNonNull(causeRequest, "causeRequest не должна быть null"), null, previousSlot, createdAt);
    }

    public static AlterationReason moved(Schedule schedule,
                                         ScheduleSlot previousSlot,
                                         Type type,
                                         LocalDateTime createdAt) {
        return new AlterationReason(schedule, previousSlot.getRequest(), type, Cause.NONE,
                null, null, previousSlot, createdAt);
    }

    private AlterationReason(Schedule schedule,
                             Request request,
                             Type type,
                             Cause cause,
                             Request causeRequest,
                             EngineerCrew causeCrew,
                             ScheduleSlot previousSlot,
                             LocalDateTime createdAt) {
        this.schedule = Objects.requireNonNull(schedule, "schedule не должен быть null");
        this.request = Objects.requireNonNull(request, "request не должен быть null");
        this.type = Objects.requireNonNull(type, "type не должен быть null");
        if (type.getCause() != cause) {
            throw new IllegalArgumentException("Type %s не описывает cause %s".formatted(type, cause));
        }
        if (type.isMovement() != (previousSlot != null)) {
            throw new IllegalArgumentException("Прежняя позиция обязательна для перемещения и запрещена для остальных типов, type %s".formatted(type));
        }
        this.causeRequest = causeRequest;
        this.causeCrew = causeCrew;
        if (previousSlot != null) {
            this.previousCrew = Objects.requireNonNull(previousSlot.getEngineerCrew(), "previousCrew не должна быть null");
            this.previousStartTime = Objects.requireNonNull(previousSlot.getStartTimeWindow(), "previousStartTime не должно быть null");
        }
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt не должен быть null");
    }

    public enum Type {
        ADDED_FOR_HIGHER_PRIORITY_REQUEST(Cause.REQUEST),
        REMOVED_FOR_HIGHER_PRIORITY_REQUEST(Cause.REQUEST),
        ADDED_AFTER_REQUEST_CANCELLATION(Cause.REQUEST),
        REMOVED_AFTER_REQUEST_CANCELLATION(Cause.REQUEST),
        ADDED_FOR_CREW_OFF_SHIFT(Cause.CREW),
        REMOVED_FOR_CREW_OFF_SHIFT(Cause.CREW),
        // заявка была у снятой бригады: прежняя бригада и есть causeCrew
        REASSIGNED_FOR_CREW_OFF_SHIFT(Cause.CREW),
        // перемещения, объяснимые без контекста пересчёта: хранят прежнюю позицию заявки
        REASSIGNED(Cause.NONE, true),
        RESCHEDULED_AFTER_INSERTION(Cause.REQUEST, true),
        RESCHEDULED_AFTER_REMOVAL(Cause.REQUEST, true),
        RESCHEDULED(Cause.NONE, true),
        NO_CREW_WITH_REQUIRED_SKILL(Cause.NONE),
        NO_CREW_WITH_REQUIRED_VEHICLE(Cause.NONE),
        NO_CAPACITY_FOR_REQUIRED_EQUIPMENT(Cause.NONE),
        NO_REQUIRED_EQUIPMENT_IN_STOCK(Cause.NONE),
        NO_AVAILABLE_TIME_SLOT(Cause.NONE);

        @Getter
        private final Cause cause;

        @Getter
        private final boolean movement;

        Type(Cause cause) {
            this(cause, false);
        }

        Type(Cause cause, boolean movement) {
            this.cause = cause;
            this.movement = movement;
        }
    }

    public enum Cause {
        REQUEST,
        CREW,
        NONE
    }
}
