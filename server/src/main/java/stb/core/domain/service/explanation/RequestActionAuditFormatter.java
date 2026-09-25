package stb.core.domain.service.explanation;

import org.springframework.stereotype.Component;
import stb.core.domain.dto.explanation.PlanSnapshot;
import stb.core.domain.dto.explanation.ReplanContext;
import stb.core.domain.dto.explanation.ReplanningExplanation;
import stb.core.domain.dto.explanation.RequestChange;
import stb.core.domain.model.RequestActionAudit;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RequestActionAuditFormatter {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public RequestActionAudit.Action primaryAction(RequestChange change) {
        if (change.has(RequestChange.Type.REASSIGNED)) {
            return RequestActionAudit.Action.ASSIGNED_CREW_CHANGE;
        }
        if (change.has(RequestChange.Type.ADDED_AS_UNASSIGNED)) {
            return RequestActionAudit.Action.REQUEST_CREATED;
        }
        if (change.has(RequestChange.Type.ADDED_TO_PLAN)
                || change.has(RequestChange.Type.ASSIGNED_FROM_UNASSIGNED)) {
            return RequestActionAudit.Action.INITIAL_ASSIGNMENT;
        }
        if (change.has(RequestChange.Type.BECAME_UNASSIGNED)
                || change.has(RequestChange.Type.REMOVED_FROM_PLAN)) {
            return RequestActionAudit.Action.REMOVE_FROM_SCHEDULE;
        }
        return RequestActionAudit.Action.SCHEDULE_CHANGE;
    }

    public String manual(RequestChange change, PlanSnapshot previousPlan, PlanSnapshot newPlan) {
        CrewNames names = CrewNames.from(previousPlan, newPlan);
        List<String> parts = new ArrayList<>();

        if (change.has(RequestChange.Type.REASSIGNED)) {
            parts.add("была переназначена с бригады «%s» на бригаду «%s»"
                    .formatted(names.name(change.previousCrewId()), names.name(change.newCrewId())));
        } else if (change.has(RequestChange.Type.ADDED_TO_PLAN)
                || change.has(RequestChange.Type.ASSIGNED_FROM_UNASSIGNED)) {
            parts.add("была назначена бригаде «%s» на %s"
                    .formatted(names.name(change.newCrewId()), formatTime(change.newStartTime())));
        } else if (change.has(RequestChange.Type.BECAME_UNASSIGNED)
                || change.has(RequestChange.Type.REMOVED_FROM_PLAN)) {
            parts.add("была снята с маршрута бригады «%s»"
                    .formatted(names.name(change.previousCrewId())));
        } else if (change.has(RequestChange.Type.ADDED_AS_UNASSIGNED)) {
            parts.add("была добавлена без назначения бригаде");
        }

        if (change.has(RequestChange.Type.ORDER_CHANGED)) {
            parts.add("порядок выполнения был изменён");
        }
        if (change.has(RequestChange.Type.PLANNED_TIME_CHANGED)) {
            parts.add("плановое время было изменено с %s на %s"
                    .formatted(formatTime(change.previousStartTime()), formatTime(change.newStartTime())));
        }

        return "После ручного изменения расписания заявка №%d %s."
                .formatted(change.requestId(), String.join("; ", parts));
    }

    public String eventDriven(ReplanContext context,
                              ReplanningExplanation.Change change,
                              PlanSnapshot previousPlan,
                              PlanSnapshot newPlan
    ) {
        String prefix = switch (context.triggerType()) {
            case EMERGENCY_REQUEST_CREATED ->
                    "После добавления аварийной заявки №%d".formatted(context.triggerSubjectId());
            case REQUEST_CANCELLED ->
                    "После отмены заявки №%d".formatted(context.triggerSubjectId());
            case CREW_BECAME_UNAVAILABLE -> "Из-за недоступности бригады «%s»"
                    .formatted(CrewNames.from(previousPlan, newPlan).name(context.triggerSubjectId()));
        };
        return prefix + " " + lowerFirst(change.message()) + terminalPeriod(change.message());
    }

    public String initialAssignment(int requestId, String crewName, LocalTime startTime) {
        return "При первичном формировании расписания заявка №%d была назначена бригаде «%s» на %s."
                .formatted(requestId, crewName, formatTime(startTime));
    }

    private static String lowerFirst(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private static String terminalPeriod(String value) {
        return value.endsWith(".") ? "" : ".";
    }

    private static String formatTime(LocalTime time) {
        return time == null ? "—" : time.format(TIME_FORMAT);
    }

    private record CrewNames(Map<Integer, String> values) {

        private static CrewNames from(PlanSnapshot previousPlan, PlanSnapshot newPlan) {
            Map<Integer, String> values = new LinkedHashMap<>();
            previousPlan.routes().forEach(route -> values.put(route.crewId(), route.crewName()));
            newPlan.routes().forEach(route -> values.put(route.crewId(), route.crewName()));
            return new CrewNames(Map.copyOf(values));
        }

        private String name(Integer crewId) {
            return crewId == null ? "неизвестная" : values.getOrDefault(crewId, "№" + crewId);
        }
    }
}
