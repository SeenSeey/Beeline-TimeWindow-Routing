package stb.core.domain.replanning;

import org.springframework.stereotype.Component;
import stb.core.domain.model.Schedule;
import stb.core.domain.model.ScheduleSlot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ScheduleVersionDiffDomainService {

    public ScheduleVersionDiff compare(Schedule previousSchedule, Schedule currentSchedule) {
        Objects.requireNonNull(previousSchedule, "previousSchedule не должно быть null");
        Objects.requireNonNull(currentSchedule, "currentSchedule не должно быть null");
        if (!sameSchedule(currentSchedule.getPreviousSchedule(), previousSchedule)) {
            throw new IllegalArgumentException("currentSchedule должно ссылаться на previousSchedule");
        }

        Map<Integer, ScheduleSlot> previousByRequest = indexRequestSlots(previousSchedule);
        Map<Integer, ScheduleSlot> currentByRequest = indexRequestSlots(currentSchedule);
        List<ScheduleSlot> added = new ArrayList<>();
        List<ScheduleSlot> removed = new ArrayList<>();
        List<ScheduleVersionDiff.MovedSlot> moved = new ArrayList<>();

        currentByRequest.forEach((requestId, currentSlot) -> {
            ScheduleSlot previousSlot = previousByRequest.get(requestId);
            if (previousSlot == null) {
                added.add(currentSlot);
            } else if (changed(previousSlot, currentSlot)) {
                moved.add(new ScheduleVersionDiff.MovedSlot(previousSlot, currentSlot));
            }
        });
        previousByRequest.forEach((requestId, previousSlot) -> {
            if (!currentByRequest.containsKey(requestId)) {
                removed.add(previousSlot);
            }
        });

        return new ScheduleVersionDiff(added, removed, moved);
    }

    private static Map<Integer, ScheduleSlot> indexRequestSlots(Schedule schedule) {
        Map<Integer, ScheduleSlot> byRequest = new LinkedHashMap<>();
        for (ScheduleSlot slot : schedule.getSlots()) {
            if (slot.getType() != ScheduleSlot.SlotType.REQUEST) {
                continue;
            }
            int requestId = slot.getRequest().getId();
            if (byRequest.put(requestId, slot) != null) {
                throw new IllegalArgumentException(
                        "Заявка №%d встречается в версии расписания больше одного раза".formatted(requestId));
            }
        }
        return byRequest;
    }

    private static boolean changed(ScheduleSlot previousSlot, ScheduleSlot currentSlot) {
        return previousSlot.getEngineerCrew().getId() != currentSlot.getEngineerCrew().getId()
                || !Objects.equals(previousSlot.getStartTimeWindow(), currentSlot.getStartTimeWindow())
                || !Objects.equals(previousSlot.getEndTimeWindow(), currentSlot.getEndTimeWindow());
    }

    private static boolean sameSchedule(Schedule actual, Schedule expected) {
        return actual == expected || actual != null && actual.getId() != 0 && actual.getId() == expected.getId();
    }
}
