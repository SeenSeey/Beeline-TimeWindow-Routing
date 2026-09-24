package stb.core.domain.dto;

import java.util.List;

public record SchedulePreviewDto(List<ScheduleTimelineDto.EngineerCrewRow> engineers,
                                 List<UnassignedRequestDto> unassignedRequests,
                                 List<Violation> violations) {

    public record Violation(int crewId, int requestId, String message) {
    }
}
