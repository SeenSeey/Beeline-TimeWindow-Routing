package stb.core.domain.dto;

import stb.core.domain.model.RequestActionAudit;

import java.time.LocalDateTime;

public record RequestHistoryItemDto(
        int id,
        LocalDateTime timestamp,
        RequestActionAudit.Action action,
        String description,
        Integer crewId,
        Integer scheduleId
) {

    public static RequestHistoryItemDto from(RequestActionAudit audit) {
        return new RequestHistoryItemDto(
                audit.getId(),
                audit.getTimestamp(),
                audit.getAction(),
                audit.getDescription(),
                audit.getAssignedCrew() == null ? null : audit.getAssignedCrew().getId(),
                audit.getSchedule() == null ? null : audit.getSchedule().getId()
        );
    }
}
