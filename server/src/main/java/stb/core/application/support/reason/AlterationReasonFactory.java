package stb.core.application.support.reason;

import org.springframework.stereotype.Component;
import stb.core.domain.model.AlterationReason;
import stb.core.domain.model.AlterationReason.Type;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Request;
import stb.core.domain.model.Schedule;
import stb.core.domain.model.ScheduleSlot;
import stb.core.domain.replanning.AutomaticReplanContext;
import stb.core.domain.replanning.ScheduleVersionDiff;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AlterationReasonFactory {

    public List<AlterationReason> from(AutomaticReplanContext context,
                                       Schedule schedule,
                                       ScheduleVersionDiff diff,
                                       LocalDateTime createdAt) {
        Map<Integer, List<Request>> previousRoutes = routesByCrewId(schedule.getPreviousSchedule());
        Map<Integer, List<Request>> currentRoutes = routesByCrewId(schedule);
        List<AlterationReason> reasons = new ArrayList<>();
        diff.added().stream().map(slot -> create(
                        context, schedule, slot.getRequest(), alterationType(context, Change.ADDED), createdAt))
                .forEach(reasons::add);
        diff.moved().stream().map(change -> moved(
                        context, schedule, change, previousRoutes, currentRoutes, createdAt))
                .forEach(reasons::add);
        diff.removed().stream().map(slot -> create(
                        context, schedule, slot.getRequest(), alterationType(context, Change.REMOVED), createdAt))
                .forEach(reasons::add);
        // заявка-причина (срочная или отменённая) сама попадает в diff, но объяснять её ссылкой на себя бессмысленно
        return reasons.stream()
                .filter(reason -> !isOwnCause(reason))
                .toList();
    }

    private static boolean isOwnCause(AlterationReason reason) {
        return reason.getCauseRequest() != null
                && reason.getCauseRequest().getId() == reason.getRequest().getId();
    }

    /**
     * Контекст пересчёта объясняет перемещение только для заявок снятой бригады; остальные перемещения —
     * каскад перестроения маршрутов, поэтому объясняются по изменению маршрута, а не контекстом.
     */
    private static AlterationReason moved(AutomaticReplanContext context,
                                          Schedule schedule,
                                          ScheduleVersionDiff.MovedSlot change,
                                          Map<Integer, List<Request>> previousRoutes,
                                          Map<Integer, List<Request>> currentRoutes,
                                          LocalDateTime createdAt) {
        ScheduleSlot previousSlot = change.previousSlot();
        ScheduleSlot currentSlot = change.currentSlot();
        EngineerCrew previousCrew = previousSlot.getEngineerCrew();

        if (previousCrew.getId() != currentSlot.getEngineerCrew().getId()) {
            if (context instanceof AutomaticReplanContext.CrewOffShift value
                    && value.causeCrew().getId() == previousCrew.getId()) {
                return AlterationReason.causedByCrew(schedule, currentSlot.getRequest(),
                        Type.REASSIGNED_FOR_CREW_OFF_SHIFT, value.causeCrew(), createdAt);
            }
            return AlterationReason.moved(schedule, previousSlot, Type.REASSIGNED, createdAt);
        }

        Request request = currentSlot.getRequest();
        List<Request> previousRoute = previousRoutes.get(previousCrew.getId());
        List<Request> currentRoute = currentRoutes.get(previousCrew.getId());
        // позже — ищем заявку, добавленную в маршрут перед этой; раньше — убранную из маршрута перед этой
        if (currentSlot.getStartTimeWindow().isAfter(previousSlot.getStartTimeWindow())) {
            Request inserted = nearestMissing(requestsBefore(currentRoute, request), previousRoute);
            return inserted == null
                    ? AlterationReason.moved(schedule, previousSlot, Type.RESCHEDULED, createdAt)
                    : AlterationReason.movedByRequest(schedule, previousSlot, Type.RESCHEDULED_AFTER_INSERTION, inserted, createdAt);
        }
        Request removed = currentSlot.getStartTimeWindow().isBefore(previousSlot.getStartTimeWindow())
                ? nearestMissing(requestsBefore(previousRoute, request), currentRoute)
                : null;
        return removed == null
                ? AlterationReason.moved(schedule, previousSlot, Type.RESCHEDULED, createdAt)
                : AlterationReason.movedByRequest(schedule, previousSlot, Type.RESCHEDULED_AFTER_REMOVAL, removed, createdAt);
    }

    private static List<Request> requestsBefore(List<Request> route, Request request) {
        List<Request> before = new ArrayList<>();
        for (Request routeRequest : route) {
            if (routeRequest.getId() == request.getId()) {
                return before;
            }
            before.add(routeRequest);
        }
        return before;
    }

    /**
     * Ближайшая к концу {@code candidates} заявка, которой нет в {@code otherRoute}; перестановки внутри маршрута не считаются.
     */
    private static Request nearestMissing(List<Request> candidates, List<Request> otherRoute) {
        Set<Integer> otherIds = otherRoute.stream().map(Request::getId).collect(Collectors.toSet());
        for (int i = candidates.size() - 1; i >= 0; i--) {
            if (!otherIds.contains(candidates.get(i).getId())) {
                return candidates.get(i);
            }
        }
        return null;
    }

    private static Map<Integer, List<Request>> routesByCrewId(Schedule schedule) {
        return schedule.getSlots().stream()
                .filter(slot -> slot.getType() == ScheduleSlot.SlotType.REQUEST)
                .sorted(Comparator.comparing(ScheduleSlot::getStartTimeWindow))
                .collect(Collectors.groupingBy(
                        slot -> slot.getEngineerCrew().getId(),
                        Collectors.mapping(ScheduleSlot::getRequest, Collectors.toList())
                ));
    }

    private static AlterationReason create(AutomaticReplanContext context,
                                           Schedule schedule,
                                           Request request,
                                           Type type,
                                           LocalDateTime createdAt) {
        return switch (context) {
            case AutomaticReplanContext.HigherPriorityRequest value ->
                    AlterationReason.causedByRequest(schedule, request, type, value.causeRequest(), createdAt);
            case AutomaticReplanContext.RequestCancellation value ->
                    AlterationReason.causedByRequest(schedule, request, type, value.causeRequest(), createdAt);
            case AutomaticReplanContext.CrewOffShift value ->
                    AlterationReason.causedByCrew(schedule, request, type, value.causeCrew(), createdAt);
        };
    }

    private static Type alterationType(AutomaticReplanContext context, Change change) {
        return switch (context) {
            case AutomaticReplanContext.HigherPriorityRequest ignored -> switch (change) {
                case ADDED -> Type.ADDED_FOR_HIGHER_PRIORITY_REQUEST;
                case REMOVED -> Type.REMOVED_FOR_HIGHER_PRIORITY_REQUEST;
            };
            case AutomaticReplanContext.RequestCancellation ignored -> switch (change) {
                case ADDED -> Type.ADDED_AFTER_REQUEST_CANCELLATION;
                case REMOVED -> Type.REMOVED_AFTER_REQUEST_CANCELLATION;
            };
            case AutomaticReplanContext.CrewOffShift ignored -> switch (change) {
                case ADDED -> Type.ADDED_FOR_CREW_OFF_SHIFT;
                case REMOVED -> Type.REMOVED_FOR_CREW_OFF_SHIFT;
            };
        };
    }

    private enum Change {
        ADDED,
        REMOVED
    }
}
