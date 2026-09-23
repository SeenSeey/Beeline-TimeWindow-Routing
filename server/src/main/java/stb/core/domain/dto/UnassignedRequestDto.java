package stb.core.domain.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import stb.core.domain.model.*;

public record UnassignedRequestDto(
    int id,
    String fullAddress,
    double latitude,
    double longitude,
    DictionaryDto status,
    boolean urgent,
    LocalDateTime deadline,
    DictionaryDto jobType,
    int jobTimeSpanMinutes,
    int maxTravelTimeSpanMinutes,
    List<DictionaryDto> requiredSkills,
    List<String> equipment,
    DictionaryDto requiredVehicle,
    LocalTime requestTimeWindowStart,
    LocalTime requestTimeWindowEnd,
    String assignedCrew,
    LocalDateTime assignedAt,
    LocalDateTime createdAt
) {
    public static UnassignedRequestDto from(Request request) {
        JobType jobType = request.getJobType();

        return new UnassignedRequestDto(
            request.getId(),
            request.getFullAddress(),
            request.getLatitude(),
            request.getLongitude(),
            Optional.ofNullable(request.getStatus()).map(DictionaryDto::from).orElse(null),
            request.isUrgent(),
            request.getDeadline(),
            DictionaryDto.from(jobType),
            jobType.getJobTimeSpanMinutes(),
            jobType.getMaxTravelTimeSpanMinutes(),
            Optional.ofNullable(jobType.getRequiredSkills())
                .orElse(List.of())
                .stream()
                .map(DictionaryDto::from)
                .toList(),
            Optional.ofNullable(request.getEquipment())
                .orElse(List.of())
                .stream()
                .map(Equipment::getDisplayName)
                .toList(),
            Optional.ofNullable(request.getRequiredVehicle()).map(DictionaryDto::from).orElse(null),
            request.getStartTimeWindow(),
            request.getEndTimeWindow(),
            Optional.ofNullable(request.getAssignedCrew()).map(EngineerCrew::getName).orElse(null),
            request.getAssignedAt(),
            request.getCreatedAt()
        );
    }
}
