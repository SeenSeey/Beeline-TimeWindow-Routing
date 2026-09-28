package stb.core.domain.dto;

import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Request;
import stb.core.domain.model.Schedule;
import stb.core.domain.model.ScheduleSlot;

import java.time.LocalTime;
import java.util.List;

public record ScheduleHistoryResult(List<StartTimeChange> startTimeChanged,
                                    List<ScheduleSlot> assigned,
                                    List<ScheduleSlot> reassigned,
                                    List<Request> unassigned,
                                    List<Request> notPlaced,
                                    List<EngineerCrew> engineerCrews,
                                    Schedule schedule) {

    /** The request's slot kept its crew, but its start time moved away from {@code previousStartTime} */
    public record StartTimeChange(ScheduleSlot slot, LocalTime previousStartTime) {
    }
}