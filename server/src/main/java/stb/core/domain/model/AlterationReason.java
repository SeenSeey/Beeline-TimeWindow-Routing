package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

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

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

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

    public static AlterationReason addedDueToCrewOffShift(Schedule schedule, Request request, EngineerCrew causeCrew) {
        return AlterationReason.builder()
                .schedule(schedule)
                .request(request)
                .type(Type.ADDED_FOR_CREW_OFF_SHIFT)
                .causeCrew(causeCrew)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static AlterationReason reassignedDueToCrewOffShift(Schedule schedule, Request request, EngineerCrew causeCrew) {
        return AlterationReason.builder()
                .schedule(schedule)
                .request(request)
                .type(Type.REASSIGNED_FOR_CREW_OFF_SHIFT)
                .causeCrew(causeCrew)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static AlterationReason removedDueToCrewOffShift(Schedule schedule, Request request, EngineerCrew causeCrew) {
        return AlterationReason.builder()
                .schedule(schedule)
                .request(request)
                .type(Type.REMOVED_FOR_CREW_OFF_SHIFT)
                .causeCrew(causeCrew)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static AlterationReason startTimeChangedDueToCrewOffShift(Schedule schedule, Request request, EngineerCrew causeCrew) {
        return AlterationReason.builder()
                .schedule(schedule)
                .request(request)
                .type(Type.TIME_CHANGED_FOR_CREW_OFF_SHIFT)
                .causeCrew(causeCrew)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public String getDescription() {
        return getType().descriptionBuilder.apply(this);
    }

    public enum Type {
        INITIAL_NO_AVAILABLE_TIME_SLOT(reason -> ""),

        ADDED_FOR_HIGHER_PRIORITY_REQUEST(reason -> ""),
        REASSIGNED_FOR_HIGHER_PRIORITY_REQUEST(reason -> ""),
        REMOVED_FOR_HIGHER_PRIORITY_REQUEST(reason -> "Заявка снята с расписания из-за срочной заявки №%d".formatted(reason.getCauseRequest().getId())),
        TIME_CHANGED_FOR_HIGHER_PRIORITY_REQUEST(reason -> ""),

        ADDED_AFTER_REQUEST_CANCELLATION(reason -> ""),
        REMOVED_AFTER_REQUEST_CANCELLATION(reason -> ""),
        ADDED_FOR_CREW_OFF_SHIFT(reason -> ""),
        REMOVED_FOR_CREW_OFF_SHIFT(reason -> ""),
        REASSIGNED_FOR_CREW_OFF_SHIFT(reason -> ""),
        TIME_CHANGED_FOR_CREW_OFF_SHIFT(reason -> ""),

        // TODO: проверить нужно ли
        // перемещения, объяснимые без контекста пересчёта: хранят прежнюю позицию заявки
        REASSIGNED(reason -> ""),
        RESCHEDULED_AFTER_INSERTION(reason -> ""),
        RESCHEDULED_AFTER_REMOVAL(reason -> ""),
        RESCHEDULED(reason -> ""),
        NO_CREW_WITH_REQUIRED_SKILL(reason -> ""),
        NO_CREW_WITH_REQUIRED_VEHICLE(reason -> ""),
        NO_CAPACITY_FOR_REQUIRED_EQUIPMENT(reason -> ""),
        NO_REQUIRED_EQUIPMENT_IN_STOCK(reason -> "");

        // TODO: проверить, нужно ли
        // NO_AVAILABLE_TIME_SLOT

        private final Function<AlterationReason, String> descriptionBuilder;

        Type(Function<AlterationReason, String> descriptionBuilder) {
            this.descriptionBuilder = descriptionBuilder;
        }

        private String formatSkillReason(Request request) {
            List<String> skills = request.getJobType().getRequiredSkills().stream()
                    .map(skill -> "«%s»".formatted(skill.getDisplayName()))
                    .toList();

            if (skills.isEmpty()) {
                return "У типа работ «%s» не указаны навыки, поэтому ни одна бригада не подходит".formatted(request.getJobType().getDisplayName());
            }

            if (skills.size() == 1) {
                return "Среди бригад на смене нет бригады с навыком %s".formatted(skills.getFirst());
            }

            return "Среди бригад на смене нет бригады ни с одним из навыков: %s".formatted(String.join(", ", skills));
        }
    }
}
