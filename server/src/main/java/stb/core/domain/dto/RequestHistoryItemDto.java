package stb.core.domain.dto;

import stb.core.domain.model.RequestActionAudit;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record RequestHistoryItemDto(
        int id,
        LocalDateTime timestamp,
        RequestActionAudit.Action action,
        String description,
        Integer crewId,
        String crewName,
        LocalTime plannedStart,
        LocalTime plannedEnd
) {

    public static RequestHistoryItemDto from(RequestActionAudit audit) {
        return new RequestHistoryItemDto(
                audit.getId(),
                audit.getTimestamp(),
                audit.getAction(),
                audit.getAction().getDisplayName(),
                audit.getAssignedCrew() == null ? null : audit.getAssignedCrew().getId(),
                audit.getAssignedCrew() == null ? null : audit.getAssignedCrew().getName(),
                audit.getPlannedStart(),
                audit.getPlannedEnd()
        );
    }
}
