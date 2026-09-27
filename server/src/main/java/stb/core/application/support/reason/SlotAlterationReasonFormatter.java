package stb.core.application.support.reason;

import org.springframework.stereotype.Component;
import stb.core.domain.model.SlotAlterationReason;

@Component
public class SlotAlterationReasonFormatter {

    public String format(SlotAlterationReason reason) {
        return switch (reason.getAction()) {
            case ADDED_FOR_HIGHER_PRIORITY_REQUEST ->
                    "Заявка добавлена в расписание из-за заявки №%d с более высоким приоритетом"
                            .formatted(reason.getCauseRequest().getId());
            case MOVED_FOR_HIGHER_PRIORITY_REQUEST ->
                    "Заявка перенесена из-за заявки №%d с более высоким приоритетом"
                            .formatted(reason.getCauseRequest().getId());
            case REMOVED_FOR_HIGHER_PRIORITY_REQUEST ->
                    "Заявка снята с расписания из-за заявки №%d с более высоким приоритетом"
                            .formatted(reason.getCauseRequest().getId());
            case ADDED_AFTER_REQUEST_CANCELLATION ->
                    "Заявка добавлена в расписание после отмены заявки №%d"
                            .formatted(reason.getCauseRequest().getId());
            case MOVED_AFTER_REQUEST_CANCELLATION ->
                    "Заявка перенесена после отмены заявки №%d"
                            .formatted(reason.getCauseRequest().getId());
            case REMOVED_AFTER_REQUEST_CANCELLATION ->
                    "Заявка снята с расписания после отмены заявки №%d"
                            .formatted(reason.getCauseRequest().getId());
            case ADDED_FOR_CREW_OFF_SHIFT ->
                    "Заявка добавлена в расписание из-за недоступности бригады «%s»"
                            .formatted(reason.getCauseCrew().getName());
            case MOVED_FOR_CREW_OFF_SHIFT ->
                    "Заявка перераспределена на бригаду «%s», так как бригада «%s» недоступна"
                            .formatted(reason.getSlot().getEngineerCrew().getName(), reason.getCauseCrew().getName());
            case REMOVED_FOR_CREW_OFF_SHIFT ->
                    "Заявка снята с расписания из-за недоступности бригады «%s»"
                            .formatted(reason.getCauseCrew().getName());
        };
    }
}
