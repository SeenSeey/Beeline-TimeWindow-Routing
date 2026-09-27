package stb.core.application.support.reason;

import org.springframework.stereotype.Component;
import stb.core.domain.model.AlterationReason;
import stb.core.domain.model.Request;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Component
public class AlterationReasonFormatter {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public String format(AlterationReason reason) {
        Request request = reason.getRequest();
        return switch (reason.getType()) {
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
                    "Заявка перераспределена, так как бригада «%s» недоступна"
                            .formatted(reason.getCauseCrew().getName());
            case REMOVED_FOR_CREW_OFF_SHIFT ->
                    "Заявка снята с расписания из-за недоступности бригады «%s»"
                            .formatted(reason.getCauseCrew().getName());
            case NO_CREW_WITH_REQUIRED_SKILL -> formatSkillReason(request);
            case NO_CREW_WITH_REQUIRED_VEHICLE ->
                    "Нет подходящей бригады с транспортом «%s»"
                            .formatted(request.getRequiredVehicle().getDisplayName());
            case NO_CAPACITY_FOR_REQUIRED_EQUIPMENT ->
                    "У подходящих бригад нет возможности взять оборудование «%s»: вместимость транспорта исчерпана"
                            .formatted(request.getRequiredEquipment().getDisplayName());
            case NO_REQUIRED_EQUIPMENT_IN_STOCK ->
                    "У подходящих бригад нет в наличии оборудования «%s»"
                            .formatted(request.getRequiredEquipment().getDisplayName());
            case NO_AVAILABLE_TIME_SLOT ->
                    "В период %s–%s у подходящих бригад не найдено свободного времени"
                            .formatted(
                                    TIME_FORMATTER.format(request.getStartTimeWindow()),
                                    TIME_FORMATTER.format(request.getEndTimeWindow())
                            );
        };
    }

    private String formatSkillReason(Request request) {
        String skills = request.getJobType().getRequiredSkills().stream()
                .map(skill -> skill.getDisplayName())
                .collect(Collectors.joining(", "));
        if (skills.isEmpty()) {
            return "Нет бригады с навыками, необходимыми для этого типа работ";
        }
        return "Нет бригады с необходимыми навыками: %s".formatted(skills);
    }
}
