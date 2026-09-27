package stb.core.domain.dto;

import stb.core.domain.model.Request;
import stb.core.domain.model.Schedule;
import stb.core.domain.model.ScheduleSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record ScheduleHistoryResult(List<ScheduleSlot> startTimeChanged,
                                    List<ScheduleSlot> assigned,
                                    List<ScheduleSlot> reassigned,
                                    List<Request> unassigned,
                                    Schedule schedule) {

    public static ScheduleHistoryResult createHistory(Schedule previousSchedule,
                                                      Schedule currentSchedule) {

        if (previousSchedule == null || currentSchedule == null) {
            throw new IllegalArgumentException("Расписание не найдено");
        }

        Map<Integer, ScheduleSlot> previousSlotsByRequestId = previousSchedule.getSlots()
                .stream()
                .collect(Collectors.toMap(ScheduleSlot::getId, Function.identity()));

        Map<Integer, ScheduleSlot> currentSlotsByRequestId = currentSchedule.getSlots()
                .stream()
                .collect(Collectors.toMap(ScheduleSlot::getId, Function.identity()));

        List<ScheduleSlot> startTimeChanged = new ArrayList<>();
        List<ScheduleSlot> assigned = new ArrayList<>();
        List<ScheduleSlot> reassigned = new ArrayList<>();
        List<Request> unassigned = new ArrayList<>();

        for (ScheduleSlot currentSlot : currentSchedule.getSlots()) {
            ScheduleSlot previousSlot = previousSlotsByRequestId.get(currentSlot.getRequest().getId());

            if (previousSlot == null) {
                assigned.add(currentSlot);
                continue;
            }

            if (!previousSlot.getStartTimeWindow().equals(currentSlot.getStartTimeWindow())) {
                startTimeChanged.add(currentSlot);
                continue;
            }

            if (previousSlot.getEngineerCrew().getId() != currentSlot.getEngineerCrew().getId()) {
                reassigned.add(currentSlot);
            }
        }

        for (ScheduleSlot previousSlot : previousSchedule.getSlots()) {
            ScheduleSlot currentSlot = currentSlotsByRequestId.get(previousSlot.getRequest().getId());

            if (currentSlot == null) {
                unassigned.add(previousSlot.getRequest());
            }
        }

        return new ScheduleHistoryResult(startTimeChanged, assigned, reassigned, unassigned, currentSchedule);
    }
}