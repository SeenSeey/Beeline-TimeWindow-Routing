package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static AlterationReason causedByRequest(Schedule schedule,
                                                   Request request,
                                                   Type type,
                                                   Request causeRequest,
                                                   LocalDateTime createdAt) {
        return new AlterationReason(schedule, request, type, Cause.REQUEST,
                Objects.requireNonNull(causeRequest, "causeRequest не должна быть null"), null, createdAt);
    }

    public static AlterationReason causedByCrew(Schedule schedule,
                                                Request request,
                                                Type type,
                                                EngineerCrew causeCrew,
                                                LocalDateTime createdAt) {
        return new AlterationReason(schedule, request, type, Cause.CREW, null,
                Objects.requireNonNull(causeCrew, "causeCrew не должна быть null"), createdAt);
    }

    public static AlterationReason unscheduled(Schedule schedule,
                                               Request request,
                                               Type type,
                                               LocalDateTime createdAt) {
        return new AlterationReason(schedule, request, type, Cause.NONE, null, null, createdAt);
    }

    private AlterationReason(Schedule schedule,
                             Request request,
                             Type type,
                             Cause cause,
                             Request causeRequest,
                             EngineerCrew causeCrew,
                             LocalDateTime createdAt) {
        this.schedule = Objects.requireNonNull(schedule, "schedule не должен быть null");
        this.request = Objects.requireNonNull(request, "request не должен быть null");
        this.type = Objects.requireNonNull(type, "type не должен быть null");
        if (type.getCause() != cause) {
            throw new IllegalArgumentException("Type %s не описывает cause %s".formatted(type, cause));
        }
        this.causeRequest = causeRequest;
        this.causeCrew = causeCrew;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt не должен быть null");
    }

    public enum Type {
        ADDED_FOR_HIGHER_PRIORITY_REQUEST(Cause.REQUEST),
        MOVED_FOR_HIGHER_PRIORITY_REQUEST(Cause.REQUEST),
        REMOVED_FOR_HIGHER_PRIORITY_REQUEST(Cause.REQUEST),
        ADDED_AFTER_REQUEST_CANCELLATION(Cause.REQUEST),
        MOVED_AFTER_REQUEST_CANCELLATION(Cause.REQUEST),
        REMOVED_AFTER_REQUEST_CANCELLATION(Cause.REQUEST),
        ADDED_FOR_CREW_OFF_SHIFT(Cause.CREW),
        MOVED_FOR_CREW_OFF_SHIFT(Cause.CREW),
        REMOVED_FOR_CREW_OFF_SHIFT(Cause.CREW),
        NO_CREW_WITH_REQUIRED_SKILL(Cause.NONE),
        NO_CREW_WITH_REQUIRED_VEHICLE(Cause.NONE),
        NO_CAPACITY_FOR_REQUIRED_EQUIPMENT(Cause.NONE),
        NO_REQUIRED_EQUIPMENT_IN_STOCK(Cause.NONE),
        NO_AVAILABLE_TIME_SLOT(Cause.NONE);

        @Getter
        private final Cause cause;

        Type(Cause cause) {
            this.cause = cause;
        }
    }

    public enum Cause {
        REQUEST,
        CREW,
        NONE
    }
}
