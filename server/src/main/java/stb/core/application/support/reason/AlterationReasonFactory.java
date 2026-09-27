package stb.core.application.support.reason;

import org.springframework.stereotype.Component;
import stb.core.domain.model.AlterationReason;
import stb.core.domain.model.AlterationReason.Type;
import stb.core.domain.model.Request;
import stb.core.domain.model.Schedule;
import stb.core.domain.replanning.AutomaticReplanContext;
import stb.core.domain.replanning.ScheduleVersionDiff;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class AlterationReasonFactory {

    public List<AlterationReason> from(AutomaticReplanContext context,
                                       Schedule schedule,
                                       ScheduleVersionDiff diff,
                                       LocalDateTime createdAt) {
        List<AlterationReason> reasons = new ArrayList<>();
        diff.added().stream().map(slot -> create(
                        context, schedule, slot.getRequest(), alterationType(context, Change.ADDED), createdAt))
                .forEach(reasons::add);
        diff.moved().stream().map(change -> create(
                        context, schedule, change.currentSlot().getRequest(), alterationType(context, Change.MOVED), createdAt))
                .forEach(reasons::add);
        diff.removed().stream().map(slot -> create(
                        context, schedule, slot.getRequest(), alterationType(context, Change.REMOVED), createdAt))
                .forEach(reasons::add);
        return List.copyOf(reasons);
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
                case MOVED -> Type.MOVED_FOR_HIGHER_PRIORITY_REQUEST;
                case REMOVED -> Type.REMOVED_FOR_HIGHER_PRIORITY_REQUEST;
            };
            case AutomaticReplanContext.RequestCancellation ignored -> switch (change) {
                case ADDED -> Type.ADDED_AFTER_REQUEST_CANCELLATION;
                case MOVED -> Type.MOVED_AFTER_REQUEST_CANCELLATION;
                case REMOVED -> Type.REMOVED_AFTER_REQUEST_CANCELLATION;
            };
            case AutomaticReplanContext.CrewOffShift ignored -> switch (change) {
                case ADDED -> Type.ADDED_FOR_CREW_OFF_SHIFT;
                case MOVED -> Type.MOVED_FOR_CREW_OFF_SHIFT;
                case REMOVED -> Type.REMOVED_FOR_CREW_OFF_SHIFT;
            };
        };
    }

    private enum Change {
        ADDED,
        MOVED,
        REMOVED
    }
}
