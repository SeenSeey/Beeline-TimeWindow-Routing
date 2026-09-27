package stb.core.application.support.reason;

import org.springframework.stereotype.Component;
import stb.core.domain.model.AlterationReason;
import stb.core.domain.model.Request;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class AlterationReasonFormatter {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public String format(AlterationReason reason) {
        Request request = reason.getRequest();
        return switch (reason.getType()) {
            case ADDED_FOR_HIGHER_PRIORITY_REQUEST ->
                    "Заявка добавлена в расписание при пересчёте после поступления срочной заявки №%d"
                            .formatted(reason.getCauseRequest().getId());
            case REMOVED_FOR_HIGHER_PRIORITY_REQUEST ->
                    "Заявка снята с расписания из-за срочной заявки №%d"
                            .formatted(reason.getCauseRequest().getId());
            case ADDED_AFTER_REQUEST_CANCELLATION ->
                    "Заявка добавлена в расписание после отмены заявки №%d"
                            .formatted(reason.getCauseRequest().getId());
            case REMOVED_AFTER_REQUEST_CANCELLATION ->
                    "Заявка снята с расписания при пересчёте после отмены заявки №%d"
                            .formatted(reason.getCauseRequest().getId());
            case ADDED_FOR_CREW_OFF_SHIFT ->
                    "Заявка добавлена в расписание при пересчёте после снятия бригады «%s» со смены"
                            .formatted(reason.getCauseCrew().getName());
            case REMOVED_FOR_CREW_OFF_SHIFT ->
                    "Заявка снята с расписания после снятия бригады «%s» со смены"
                            .formatted(reason.getCauseCrew().getName());
            case REASSIGNED_FOR_CREW_OFF_SHIFT ->
                    "Заявка передана другой бригаде: бригада «%s» снята со смены"
                            .formatted(reason.getCauseCrew().getName());
            case REASSIGNED ->
                    "Заявка передана от бригады «%s» при перестроении маршрутов"
                            .formatted(reason.getPreviousCrew().getName());
            case RESCHEDULED_AFTER_INSERTION ->
                    "Визит перенесён на более позднее время (вместо %s): в маршрут бригады перед этой заявкой добавили заявку №%d"
                            .formatted(TIME_FORMATTER.format(reason.getPreviousStartTime()),
                                    reason.getCauseRequest().getId());
            case RESCHEDULED_AFTER_REMOVAL ->
                    "Визит перенесён на более раннее время (вместо %s): из маршрута бригады убрали заявку №%d, которая стояла перед этой"
                            .formatted(TIME_FORMATTER.format(reason.getPreviousStartTime()),
                                    reason.getCauseRequest().getId());
            case RESCHEDULED ->
                    "Визит перенесён (вместо %s) при перестроении маршрута бригады"
                            .formatted(TIME_FORMATTER.format(reason.getPreviousStartTime()));
            case NO_CREW_WITH_REQUIRED_SKILL -> formatSkillReason(request);
            case NO_CREW_WITH_REQUIRED_VEHICLE ->
                    "Среди бригад с нужными навыками нет бригады с транспортом «%s»"
                            .formatted(request.getRequiredVehicle().getDisplayName());
            case NO_CAPACITY_FOR_REQUIRED_EQUIPMENT ->
                    "Подходящие бригады не могут взять оборудование «%s»: транспорт уже заполнен оборудованием для других заявок"
                            .formatted(request.getRequiredEquipment().getDisplayName());
            case NO_REQUIRED_EQUIPMENT_IN_STOCK ->
                    "У подходящих бригад не осталось свободного оборудования «%s»"
                            .formatted(request.getRequiredEquipment().getDisplayName());
            case NO_AVAILABLE_TIME_SLOT ->
                    "У подходящих бригад не нашлось свободного времени в окне визита %s–%s"
                            .formatted(
                                    TIME_FORMATTER.format(request.getStartTimeWindow()),
                                    TIME_FORMATTER.format(request.getEndTimeWindow())
                            );
        };
    }

    // бригаде достаточно любого из навыков типа работ — как в solver и анализаторе
    private String formatSkillReason(Request request) {
        List<String> skills = request.getJobType().getRequiredSkills().stream()
                .map(skill -> "«%s»".formatted(skill.getDisplayName()))
                .toList();
        if (skills.isEmpty()) {
            return "У типа работ «%s» не указаны навыки, поэтому ни одна бригада не подходит"
                    .formatted(request.getJobType().getDisplayName());
        }
        if (skills.size() == 1) {
            return "Среди бригад на смене нет бригады с навыком %s".formatted(skills.getFirst());
        }
        return "Среди бригад на смене нет бригады ни с одним из навыков: %s".formatted(String.join(", ", skills));
    }
}
