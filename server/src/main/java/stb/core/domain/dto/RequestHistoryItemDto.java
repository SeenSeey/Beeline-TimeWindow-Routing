package stb.core.domain.dto;

import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.RequestActionAudit;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

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
                Optional.ofNullable(audit.getAssignedCrew()).map(EngineerCrew::getId).orElse(null),
                Optional.ofNullable(audit.getAssignedCrew()).map(EngineerCrew::getName).orElse(null),
                audit.getPlannedStart(),
                audit.getPlannedEnd()
        );
    }
}
