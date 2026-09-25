package stb.core.domain.service.explanation;

import stb.core.domain.dto.explanation.PlanDiff;
import stb.core.domain.dto.explanation.PlanSnapshot;
import stb.core.domain.dto.explanation.ReplanContext;
import stb.core.domain.dto.explanation.ReplanContext.TriggerType;
import stb.core.domain.dto.explanation.ReplanningExplanation;
import stb.core.domain.dto.explanation.RequestChange;
import stb.core.domain.dto.explanation.RequestChange.Type;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ReplanningExplanationDomainService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public ReplanningExplanation explain(ReplanContext context,
                                         PlanDiff diff,
                                         PlanSnapshot previousPlan,
                                         PlanSnapshot newPlan
    ) {
        validatePlanIdentity(context, diff, previousPlan, newPlan);

        CrewNames crewNames = CrewNames.from(previousPlan, newPlan);
        List<RequestChange> requestChanges = new ArrayList<>(diff.requestChanges());
        requestChanges.sort(Comparator.comparingInt((RequestChange change) -> priority(context, change))
                .thenComparingInt(RequestChange::requestId));

        List<ReplanningExplanation.Change> changes = requestChanges.stream()
                .map(change -> new ReplanningExplanation.Change(
                        change.requestId(),
                        change.types(),
                        message(context, change, crewNames),
                        change.previousCrewId(),
                        change.newCrewId(),
                        change.previousStartTime(),
                        change.newStartTime()))
                .toList();

        return new ReplanningExplanation(
                context.triggerType(),
                title(context.triggerType()),
                summary(context, requestChanges, crewNames),
                context.occurredAt(),
                context.triggerSubjectId(),
                counters(requestChanges),
                changes
        );
    }

    private static String title(TriggerType triggerType) {
        return switch (triggerType) {
            case EMERGENCY_REQUEST_CREATED -> "План перестроен из-за аварийной заявки";
            case REQUEST_CANCELLED -> "План перестроен после отмены заявки";
            case CREW_BECAME_UNAVAILABLE -> "План перестроен из-за недоступности бригады";
        };
    }

    private static String summary(ReplanContext context,
                                  List<RequestChange> changes,
                                  CrewNames crewNames
    ) {
        return switch (context.triggerType()) {
            case EMERGENCY_REQUEST_CREATED -> emergencySummary(context, changes, crewNames);
            case REQUEST_CANCELLED -> cancellationSummary(context, changes);
            case CREW_BECAME_UNAVAILABLE -> crewUnavailableSummary(context, changes, crewNames);
        };
    }

    private static String emergencySummary(ReplanContext context,
                                           List<RequestChange> changes,
                                           CrewNames crewNames
    ) {
        Optional<RequestChange> trigger = changes.stream()
                .filter(change -> change.requestId() == context.triggerSubjectId())
                .findFirst();
        String result = trigger
                .filter(change -> change.has(Type.ADDED_TO_PLAN) || change.has(Type.ASSIGNED_FROM_UNASSIGNED))
                .map(change -> "Аварийная заявка №%d назначена бригаде «%s»."
                        .formatted(context.triggerSubjectId(), crewNames.name(change.newCrewId())))
                .orElseGet(() -> trigger
                        .filter(change -> change.has(Type.ADDED_AS_UNASSIGNED) || change.has(Type.BECAME_UNASSIGNED))
                        .map(change -> "Аварийная заявка №%d осталась без исполнителя."
                                .formatted(context.triggerSubjectId()))
                        .orElseGet(() -> "План перестроен после появления аварийной заявки №%d."
                                .formatted(context.triggerSubjectId())));
        int otherChanges = changes.size() - (trigger.isPresent() ? 1 : 0);
        if (changes.isEmpty()) {
            return result + " Изменений в плане нет.";
        }
        return otherChanges == 0 ? result : result + " Изменено других заявок: " + otherChanges + ".";
    }

    private static String cancellationSummary(ReplanContext context, List<RequestChange> changes) {
        boolean cancelledRequestRemoved = changes.stream()
                .anyMatch(change -> change.requestId() == context.triggerSubjectId()
                        && change.has(Type.REMOVED_FROM_PLAN));
        long otherChanges = changes.stream()
                .filter(change -> change.requestId() != context.triggerSubjectId())
                .count();
        String result = cancelledRequestRemoved
                ? "Отменённая заявка №%d удалена из плана.".formatted(context.triggerSubjectId())
                : "План перестроен после отмены заявки №%d.".formatted(context.triggerSubjectId());
        return otherChanges == 0
                ? result + " Другие назначения и порядок заявок не изменились."
                : result + " Изменено других заявок: " + otherChanges + ".";
    }

    private static String crewUnavailableSummary(ReplanContext context,
                                                 List<RequestChange> changes,
                                                 CrewNames crewNames
    ) {
        long affected = changes.stream()
                .filter(change -> Objects.equals(change.previousCrewId(), context.triggerSubjectId()))
                .count();
        String result = "Бригада «%s» стала недоступна."
                .formatted(crewNames.name(context.triggerSubjectId()));
        return changes.isEmpty()
                ? result + " Изменений в плане нет."
                : result + " Заявок из её маршрута изменено: " + affected + ".";
    }

    private static int priority(ReplanContext context, RequestChange change) {
        return switch (context.triggerType()) {
            case EMERGENCY_REQUEST_CREATED, REQUEST_CANCELLED ->
                    change.requestId() == context.triggerSubjectId() ? 0 : 1;
            case CREW_BECAME_UNAVAILABLE ->
                    Objects.equals(change.previousCrewId(), context.triggerSubjectId()) ? 0 : 1;
        };
    }

    private static String message(ReplanContext context, RequestChange change, CrewNames crewNames) {
        EnumSet<Type> remainingTypes = EnumSet.copyOf(change.types());
        List<String> parts = new ArrayList<>();

        switch (context.triggerType()) {
            case EMERGENCY_REQUEST_CREATED -> {
                if (change.requestId() == context.triggerSubjectId()) {
                    if (remainingTypes.remove(Type.ADDED_TO_PLAN)) {
                        parts.add("Аварийная заявка №%d добавлена в маршрут бригады «%s» на %s"
                                .formatted(change.requestId(), crewNames.name(change.newCrewId()),
                                        formatTime(change.newStartTime())));
                    } else if (remainingTypes.remove(Type.ADDED_AS_UNASSIGNED)) {
                        parts.add("Для аварийной заявки №%d не найден исполнитель; она осталась неназначенной"
                                .formatted(change.requestId()));
                    }
                }
            }
            case REQUEST_CANCELLED -> {
                if (change.requestId() == context.triggerSubjectId()
                        && remainingTypes.remove(Type.REMOVED_FROM_PLAN)) {
                    parts.add("Отменённая заявка №%d удалена из маршрута%s"
                            .formatted(change.requestId(), crewSuffix(change.previousCrewId(), crewNames)));
                }
            }
            case CREW_BECAME_UNAVAILABLE -> {
                if (Objects.equals(change.previousCrewId(), context.triggerSubjectId())) {
                    if (remainingTypes.remove(Type.REASSIGNED)) {
                        parts.add("Заявка №%d переназначена бригаде «%s»"
                                .formatted(change.requestId(), crewNames.name(change.newCrewId())));
                    } else if (remainingTypes.remove(Type.BECAME_UNASSIGNED)) {
                        parts.add("Заявка №%d осталась без исполнителя".formatted(change.requestId()));
                    }
                }
            }
        }

        for (Type type : remainingTypes) {
            parts.add(messagePart(type, change, crewNames));
        }
        return String.join("; ", parts);
    }

    private static String messagePart(Type type, RequestChange change, CrewNames crewNames) {
        return switch (type) {
            case ADDED_TO_PLAN -> "Заявка №%d добавлена в маршрут бригады «%s» на %s"
                    .formatted(change.requestId(), crewNames.name(change.newCrewId()), formatTime(change.newStartTime()));
            case ADDED_AS_UNASSIGNED -> "Для заявки №%d не найден исполнитель; она осталась неназначенной"
                    .formatted(change.requestId());
            case REMOVED_FROM_PLAN -> "Заявка №%d удалена из маршрута%s"
                    .formatted(change.requestId(), crewSuffix(change.previousCrewId(), crewNames));
            case ASSIGNED_FROM_UNASSIGNED ->
                    "Заявка №%d, ранее остававшаяся без исполнителя, добавлена в маршрут бригады «%s»"
                            .formatted(change.requestId(), crewNames.name(change.newCrewId()));
            case BECAME_UNASSIGNED -> "Заявка №%d после пересчёта осталась без исполнителя"
                    .formatted(change.requestId());
            case REASSIGNED -> "Заявка №%d переназначена с бригады «%s» на бригаду «%s»"
                    .formatted(change.requestId(), crewNames.name(change.previousCrewId()),
                            crewNames.name(change.newCrewId()));
            case ORDER_CHANGED -> "Порядок выполнения заявки №%d изменён".formatted(change.requestId());
            case PLANNED_TIME_CHANGED -> "начало заявки №%d сдвинуто с %s на %s"
                    .formatted(change.requestId(), formatTime(change.previousStartTime()),
                            formatTime(change.newStartTime()));
        };
    }

    private static ReplanningExplanation.Counters counters(List<RequestChange> changes) {
        return new ReplanningExplanation.Counters(
                count(changes, Type.ADDED_TO_PLAN, Type.ASSIGNED_FROM_UNASSIGNED),
                count(changes, Type.ADDED_AS_UNASSIGNED, Type.BECAME_UNASSIGNED),
                count(changes, Type.REASSIGNED),
                count(changes, Type.ORDER_CHANGED),
                count(changes, Type.PLANNED_TIME_CHANGED)
        );
    }

    private static int count(List<RequestChange> changes, Type... types) {
        return (int) changes.stream()
                .filter(change -> {
                    for (Type type : types) {
                        if (change.has(type)) {
                            return true;
                        }
                    }
                    return false;
                })
                .count();
    }

    private static String crewSuffix(Integer crewId, CrewNames crewNames) {
        return crewId == null ? "" : " бригады «" + crewNames.name(crewId) + "»";
    }

    private static String formatTime(LocalTime time) {
        return time == null ? "—" : time.format(TIME_FORMAT);
    }

    private static void validatePlanIdentity(ReplanContext context,
                                             PlanDiff diff,
                                             PlanSnapshot previousPlan,
                                             PlanSnapshot newPlan
    ) {
        if (context.previousPlanId() != previousPlan.planId()
                || context.newPlanId() != newPlan.planId()
                || diff.previousPlanId() != previousPlan.planId()
                || diff.newPlanId() != newPlan.planId()
                || !Objects.equals(newPlan.previousPlanId(), previousPlan.planId())) {
            throw new IllegalArgumentException("Context, diff и snapshots должны относиться к одной паре версий плана");
        }
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
