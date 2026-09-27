package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

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
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
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

    public static AlterationReason initialNoAvailableTimeSlots(Schedule schedule, Request request) {
        return AlterationReason.builder()
                .schedule(schedule)
                .request(request)
                .type(Type.INITIAL_NO_AVAILABLE_TIME_SLOT)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static AlterationReason addedDueToEmergency(Schedule schedule, Request request, Request causeRequest) {
        return AlterationReason.builder()
                .schedule(schedule)
                .request(request)
                .type(Type.ADDED_FOR_HIGHER_PRIORITY_REQUEST)
                .causeRequest(causeRequest)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static AlterationReason reassignedDueToEmergency(Schedule schedule, Request request, Request causeRequest) {
        return AlterationReason.builder()
                .schedule(schedule)
                .request(request)
                .type(Type.REASSIGNED_FOR_HIGHER_PRIORITY_REQUEST)
                .causeRequest(causeRequest)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static AlterationReason removedDueToEmergency(Schedule schedule, Request request, Request causeRequest) {
        return AlterationReason.builder()
                .schedule(schedule)
                .request(request)
                .type(Type.REMOVED_FOR_HIGHER_PRIORITY_REQUEST)
                .causeRequest(causeRequest)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static AlterationReason startTimeChangedDueToEmergency(Schedule schedule, Request request, Request causeRequest) {
        return AlterationReason.builder()
                .schedule(schedule)
                .request(request)
                .type(Type.TIME_CHANGED_FOR_HIGHER_PRIORITY_REQUEST)
                .causeRequest(causeRequest)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public enum Type {
        INITIAL_NO_AVAILABLE_TIME_SLOT,

        ADDED_FOR_HIGHER_PRIORITY_REQUEST,
        REASSIGNED_FOR_HIGHER_PRIORITY_REQUEST,
        REMOVED_FOR_HIGHER_PRIORITY_REQUEST,
        TIME_CHANGED_FOR_HIGHER_PRIORITY_REQUEST,

        ADDED_AFTER_REQUEST_CANCELLATION,
        REMOVED_AFTER_REQUEST_CANCELLATION,
        ADDED_FOR_CREW_OFF_SHIFT,
        REMOVED_FOR_CREW_OFF_SHIFT,
        REASSIGNED_FOR_CREW_OFF_SHIFT,
        // перемещения, объяснимые без контекста пересчёта: хранят прежнюю позицию заявки
        REASSIGNED,
        RESCHEDULED_AFTER_INSERTION,
        RESCHEDULED_AFTER_REMOVAL,
        RESCHEDULED,
        NO_CREW_WITH_REQUIRED_SKILL,
        NO_CREW_WITH_REQUIRED_VEHICLE,
        NO_CAPACITY_FOR_REQUIRED_EQUIPMENT,
        NO_REQUIRED_EQUIPMENT_IN_STOCK,

        // TODO: проверить, нужно ли
//        NO_AVAILABLE_TIME_SLOT
    }
}
