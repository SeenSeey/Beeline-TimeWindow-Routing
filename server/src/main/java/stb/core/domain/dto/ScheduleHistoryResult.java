package stb.core.domain.dto;

import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Request;
import stb.core.domain.model.Schedule;
import stb.core.domain.model.ScheduleSlot;
import stb.core.domain.port.route.dto.RouteDto;

import java.time.LocalTime;
import java.util.List;

public record ScheduleHistoryResult(List<StartTimeChange> startTimeChanged,
                                    List<ScheduleSlot> assigned,
                                    List<ScheduleSlot> reassigned,
                                    List<Request> unassigned,
                                    List<Request> notPlaced,
                                    List<EngineerCrew> engineerCrews,
                                    Schedule schedule) {

    public record StartTimeChange(ScheduleSlot slot, LocalTime previousStartTime) {
    }
}