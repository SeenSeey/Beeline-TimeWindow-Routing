package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
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

    public static AlterationReason notPlaced(Schedule schedule, Request request, List<EngineerCrew> crewsOnShift) {
        List<EngineerCrew> skilledCrews = crewsOnShift.stream()
                .filter(crew -> !Collections.disjoint(crew.getSkills(), request.getJobType().getRequiredSkills()))
                .toList();

        Type type = Type.INITIAL_NO_AVAILABLE_TIME_SLOT;
        if (skilledCrews.isEmpty()) {
            type = Type.NO_CREW_WITH_REQUIRED_SKILL;
        } else if (request.getRequiredVehicle() != null && skilledCrews.stream()
                .noneMatch(crew -> crew.getVehicle().getKeyword() == request.getRequiredVehicle().getKeyword())) {
            type = Type.NO_CREW_WITH_REQUIRED_VEHICLE;
        } else if (request.isUrgent()) {
            type = Type.URGENT_START_LIMIT_EXCEEDED;
        }

        return AlterationReason.builder()
                .schedule(schedule)
                .request(request)
                .type(type)
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

    public static AlterationReason startTimeChangedDueToEmergency(Schedule schedule, Request request, LocalTime previousStartTime, Request causeRequest) {
        return AlterationReason.builder()
                .schedule(schedule)
                .request(request)
                .type(Type.TIME_CHANGED_FOR_HIGHER_PRIORITY_REQUEST)
                .previousStartTime(previousStartTime)
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

    public static AlterationReason startTimeChangedDueToCrewOffShift(Schedule schedule, Request request, LocalTime previousStartTime, EngineerCrew causeCrew) {
        return AlterationReason.builder()
                .schedule(schedule)
                .request(request)
                .type(Type.TIME_CHANGED_FOR_CREW_OFF_SHIFT)
                .previousStartTime(previousStartTime)
                .causeCrew(causeCrew)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static AlterationReason addedAfterRequestCancellation(Schedule schedule, Request request, Request causeRequest) {
        return AlterationReason.builder()
                .schedule(schedule)
                .request(request)
                .type(Type.ADDED_AFTER_REQUEST_CANCELLATION)
                .causeRequest(causeRequest)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static AlterationReason reassignedAfterRequestCancellation(Schedule schedule, Request request, Request causeRequest) {
        return AlterationReason.builder()
                .schedule(schedule)
                .request(request)
                .type(Type.REASSIGNED_AFTER_REQUEST_CANCELLATION)
                .causeRequest(causeRequest)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static AlterationReason removedAfterRequestCancellation(Schedule schedule, Request request, Request causeRequest) {
        return AlterationReason.builder()
                .schedule(schedule)
                .request(request)
                .type(Type.REMOVED_AFTER_REQUEST_CANCELLATION)
                .causeRequest(causeRequest)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static AlterationReason startTimeChangedAfterRequestCancellation(Schedule schedule, Request request, LocalTime previousStartTime, Request causeRequest) {
        return AlterationReason.builder()
                .schedule(schedule)
                .request(request)
                .type(Type.TIME_CHANGED_AFTER_REQUEST_CANCELLATION)
                .previousStartTime(previousStartTime)
                .causeRequest(causeRequest)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public String getDescription() {
        return getType().descriptionBuilder.apply(this);
    }

    public enum Type {
        INITIAL_NO_AVAILABLE_TIME_SLOT(reason -> formatNoTimeSlotReason(reason.getRequest())),
        NO_CREW_WITH_REQUIRED_SKILL(reason -> formatSkillReason(reason.getRequest())),
        NO_CREW_WITH_REQUIRED_VEHICLE(reason -> "Среди бригад на смене с нужным навыком нет бригады с транспортом «%s»".formatted(reason.getRequest().getRequiredVehicle().getDisplayName())),
        URGENT_START_LIMIT_EXCEEDED(reason -> formatUrgentStartLimitReason(reason.getRequest())),

        ADDED_FOR_HIGHER_PRIORITY_REQUEST(reason -> "Заявка добавлена в расписание при пересчёте после поступления срочной заявки №%d".formatted(reason.getCauseRequest().getId())),
        REASSIGNED_FOR_HIGHER_PRIORITY_REQUEST(reason -> "Заявка передана другой бригаде при пересчёте после поступления срочной заявки №%d".formatted(reason.getCauseRequest().getId())),
        REMOVED_FOR_HIGHER_PRIORITY_REQUEST(reason -> "Заявка снята с расписания при пересчёте после поступления срочной заявки №%d".formatted(reason.getCauseRequest().getId())),
        TIME_CHANGED_FOR_HIGHER_PRIORITY_REQUEST(reason -> "Визит перенесён (вместо %s) при пересчёте после поступления срочной заявки №%d".formatted(TIME_FORMATTER.format(reason.getPreviousStartTime()), reason.getCauseRequest().getId())),

        ADDED_AFTER_REQUEST_CANCELLATION(reason -> "Заявка добавлена в расписание при пересчёте после отмены заявки №%d".formatted(reason.getCauseRequest().getId())),
        REMOVED_AFTER_REQUEST_CANCELLATION(reason -> "Заявка снята с расписания при пересчёте после отмены заявки №%d".formatted(reason.getCauseRequest().getId())),
        REASSIGNED_AFTER_REQUEST_CANCELLATION(reason -> "Заявка передана другой бригаде при пересчёте после отмены заявки №%d".formatted(reason.getCauseRequest().getId())),
        TIME_CHANGED_AFTER_REQUEST_CANCELLATION(reason -> "Визит перенесён (вместо %s) при пересчёте после отмены заявки №%d".formatted(TIME_FORMATTER.format(reason.getPreviousStartTime()), reason.getCauseRequest().getId())),

        ADDED_FOR_CREW_OFF_SHIFT(reason -> "Заявка добавлена в расписание при пересчёте после снятия бригады «%s» со смены".formatted(reason.getCauseCrew().getName())),
        REMOVED_FOR_CREW_OFF_SHIFT(reason -> "Заявка снята с расписания при пересчёте после снятия бригады «%s» со смены".formatted(reason.getCauseCrew().getName())),
        REASSIGNED_FOR_CREW_OFF_SHIFT(reason -> "Заявка передана другой бригаде при пересчёте после снятия бригады «%s» со смены".formatted(reason.getCauseCrew().getName())),
        TIME_CHANGED_FOR_CREW_OFF_SHIFT(reason -> "Визит перенесён (вместо %s) при пересчёте после снятия бригады «%s» со смены".formatted(TIME_FORMATTER.format(reason.getPreviousStartTime()), reason.getCauseCrew().getName()));

        private final Function<AlterationReason, String> descriptionBuilder;

        Type(Function<AlterationReason, String> descriptionBuilder) {
            this.descriptionBuilder = descriptionBuilder;
        }

        // notPlaced doesn't check the crews' equipment, so for a request that needs it the stock may be the cause too
        private static String formatNoTimeSlotReason(Request request) {
            String reason = "У подходящих бригад не нашлось свободного времени в окне визита %s–%s".formatted(request.getStartTimeWindow(), request.getEndTimeWindow());

            if (request.getRequiredEquipment() == null) {
                return reason;
            }

            return "%s или оборудования «%s»".formatted(reason, request.getRequiredEquipment().getDisplayName());
        }

        private static String formatUrgentStartLimitReason(Request request) {
            return "Срочная заявка не распределена: подходящие бригады не могут начать выполнение в течение %d минут после создания заявки (до %s)".formatted(
                    Request.URGENT_START_LIMIT.toMinutes(),
                    TIME_FORMATTER.format(request.getCreatedAt().plus(Request.URGENT_START_LIMIT)));
        }

        private static String formatSkillReason(Request request) {
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
