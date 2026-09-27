package stb.core.domain.dto;

import stb.core.domain.model.Request;
import stb.core.domain.model.Schedule;
import stb.core.domain.model.ScheduleSlot;

import java.util.List;

public record ScheduleHistoryResult(List<ScheduleSlot> startTimeChanged,
                                    List<ScheduleSlot> assigned,
                                    List<ScheduleSlot> reassigned,
                                    List<Request> unassigned,
                                    Schedule schedule) {
}