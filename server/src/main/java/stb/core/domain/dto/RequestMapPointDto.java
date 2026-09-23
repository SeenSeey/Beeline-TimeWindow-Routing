package stb.core.domain.dto;

import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Request;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/** A request as a point on the map: coordinates plus what the marker and its card show */
public record RequestMapPointDto(
        int id,
        String fullAddress,
        double latitude,
        double longitude,
        DictionaryDto status,
        boolean urgent,
        DictionaryDto jobType,
        LocalDateTime deadline,
        LocalTime requestTimeWindowStart,
        LocalTime requestTimeWindowEnd,
        String assignedCrew
) {

    public static RequestMapPointDto from(Request request) {
        return new RequestMapPointDto(
                request.getId(),
                request.getFullAddress(),
                request.getLatitude(),
                request.getLongitude(),
                Optional.ofNullable(request.getStatus()).map(DictionaryDto::from).orElse(null),
                request.isUrgent(),
                DictionaryDto.from(request.getJobType()),
                request.getDeadline(),
                request.getStartTimeWindow(),
                request.getEndTimeWindow(),
                Optional.ofNullable(request.getAssignedCrew()).map(EngineerCrew::getName).orElse(null)
        );
    }
}
