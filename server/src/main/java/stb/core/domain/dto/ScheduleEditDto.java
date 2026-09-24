package stb.core.domain.dto;

import java.time.LocalTime;
import java.util.List;

public record ScheduleEditDto(List<CrewRoute> routes) {

    public record CrewRoute(int crewId, List<Stop> stops) {
    }

    public record Stop(int requestId, LocalTime startTime) {
    }
}
