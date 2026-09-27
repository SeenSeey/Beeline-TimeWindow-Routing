package stb.core.application.support.reason;

import org.springframework.stereotype.Component;
import stb.core.domain.model.SlotAlterationReason;
import stb.core.domain.model.SlotAlterationReason.Action;
import stb.core.domain.model.ScheduleSlot;
import stb.core.domain.replanning.AutomaticReplanContext;
import stb.core.domain.replanning.ScheduleVersionDiff;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class SlotAlterationReasonFactory {

    public List<SlotAlterationReason> from(AutomaticReplanContext context,
                                           ScheduleVersionDiff diff,
                                           LocalDateTime createdAt) {
        List<SlotAlterationReason> reasons = new ArrayList<>();
        diff.added().stream().map(slot -> create(context, slot, alterationAction(context, Change.ADDED), createdAt))
                .forEach(reasons::add);
        diff.moved().stream().map(change -> create(
                        context, change.currentSlot(), alterationAction(context, Change.MOVED), createdAt))
                .forEach(reasons::add);
        diff.removed().stream().map(slot -> create(context, slot, alterationAction(context, Change.REMOVED), createdAt))
                .forEach(reasons::add);
        return List.copyOf(reasons);
    }

    private static SlotAlterationReason create(AutomaticReplanContext context,
                                               ScheduleSlot slot,
                                               Action action,
                                               LocalDateTime createdAt) {
        return switch (context) {
            case AutomaticReplanContext.HigherPriorityRequest value ->
                    SlotAlterationReason.causedByRequest(slot, action, value.causeRequest(), createdAt);
            case AutomaticReplanContext.RequestCancellation value ->
                    SlotAlterationReason.causedByRequest(slot, action, value.causeRequest(), createdAt);
            case AutomaticReplanContext.CrewOffShift value ->
                    SlotAlterationReason.causedByCrew(slot, action, value.causeCrew(), createdAt);
        };
    }

    private static Action alterationAction(AutomaticReplanContext context, Change change) {
        return switch (context) {
            case AutomaticReplanContext.HigherPriorityRequest ignored -> switch (change) {
                case ADDED -> Action.ADDED_FOR_HIGHER_PRIORITY_REQUEST;
                case MOVED -> Action.MOVED_FOR_HIGHER_PRIORITY_REQUEST;
                case REMOVED -> Action.REMOVED_FOR_HIGHER_PRIORITY_REQUEST;
            };
            case AutomaticReplanContext.RequestCancellation ignored -> switch (change) {
                case ADDED -> Action.ADDED_AFTER_REQUEST_CANCELLATION;
                case MOVED -> Action.MOVED_AFTER_REQUEST_CANCELLATION;
                case REMOVED -> Action.REMOVED_AFTER_REQUEST_CANCELLATION;
            };
            case AutomaticReplanContext.CrewOffShift ignored -> switch (change) {
                case ADDED -> Action.ADDED_FOR_CREW_OFF_SHIFT;
                case MOVED -> Action.MOVED_FOR_CREW_OFF_SHIFT;
                case REMOVED -> Action.REMOVED_FOR_CREW_OFF_SHIFT;
            };
        };
    }

    private enum Change {
        ADDED,
        MOVED,
        REMOVED
    }
}
