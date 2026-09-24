package stb.core.domain.dto;

import stb.core.domain.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public record CrewDetailedDto(
        int id,
        String name,
        RegionDto region,
        LocalTime shiftStartTime,
        LocalTime shiftEndTime,
        VehicleDto vehicle,
        List<DictionaryDto> skills,
        List<String> equipment,
        LocalDate routeDate,
        List<RouteVisitDto> route,
        List<AssignedRequestDto> assignedRequests
) {

    public static CrewDetailedDto from(EngineerCrew crew,
                                       LocalDate routeDate,
                                       List<ScheduleSlot> routeSlots,
                                       List<Request> assignedRequests) {
        return new CrewDetailedDto(
                crew.getId(),
                crew.getName(),
                RegionDto.from(crew.getRegion()),
                crew.getShiftStartTime(),
                crew.getShiftEndTime(),
                Optional.ofNullable(crew.getVehicle()).map(VehicleDto::from).orElse(null),
                Optional.ofNullable(crew.getSkills()).orElse(List.of()).stream().map(DictionaryDto::from).toList(),
                Optional.ofNullable(crew.getEquipment()).orElse(List.of()).stream().map(Equipment::getDisplayName).toList(),
                routeDate,
                routeSlots.stream().map(RouteVisitDto::from).toList(),
                assignedRequests.stream().map(AssignedRequestDto::from).toList()
        );
    }

    public record VehicleDto(String keyword, String displayName, double averageSpeedKmh) {

        public static VehicleDto from(Vehicle vehicle) {
            return new VehicleDto(vehicle.getKeyword().name(), vehicle.getDisplayName(), vehicle.getAverageSpeedKmh());
        }
    }

    public record RouteVisitDto(
            int requestId,
            String fullAddress,
            double latitude,
            double longitude,
            DictionaryDto jobType,
            DictionaryDto status,
            boolean urgent,
            LocalTime arrivalTime,
            LocalTime startTime,
            LocalTime endTime,
            LocalTime requestTimeWindowStart,
            LocalTime requestTimeWindowEnd,
            LocalDateTime finishedAt
    ) {

        public static RouteVisitDto from(ScheduleSlot slot) {
            Request request = slot.getRequest();
            return new RouteVisitDto(
                    request.getId(),
                    request.getFullAddress(),
                    request.getLatitude(),
                    request.getLongitude(),
                    DictionaryDto.from(request.getJobType()),
                    Optional.ofNullable(request.getStatus()).map(DictionaryDto::from).orElse(null),
                    request.isUrgent(),
                    slot.getArrivalTime(),
                    slot.getStartTimeWindow(),
                    slot.getEndTimeWindow(),
                    request.getStartTimeWindow(),
                    request.getEndTimeWindow(),
                    slot.getFinishedAt()
            );
        }
    }

    public record AssignedRequestDto(
            int id,
            String fullAddress,
            DictionaryDto status,
            boolean urgent,
            DictionaryDto jobType,
            LocalDateTime deadline,
            LocalDateTime assignedAt,
            LocalDateTime completedAt,
            LocalDateTime canceledAt
    ) {

        public static AssignedRequestDto from(Request request) {
            return new AssignedRequestDto(
                    request.getId(),
                    request.getFullAddress(),
                    Optional.ofNullable(request.getStatus()).map(DictionaryDto::from).orElse(null),
                    request.isUrgent(),
                    DictionaryDto.from(request.getJobType()),
                    request.getDeadline(),
                    request.getAssignedAt(),
                    request.getCompletedAt(),
                    request.getCanceledAt()
            );
        }
    }
}
