package stb.core.domain.dto;

import stb.core.domain.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public record RequestDetailedDto(
        int id,
        String fullAddress,
        double latitude,
        double longitude,
        DictionaryDto status,
        boolean urgent,
        LocalDateTime deadline,
        RegionDto region,
        JobTypeDto jobType,
        DictionaryDto requiredVehicle,
        List<String> equipment,
        LocalTime requestTimeWindowStart,
        LocalTime requestTimeWindowEnd,
        CrewDto assignedCrew,
        LocalDateTime assignedAt,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        LocalDateTime canceledAt,
        List<ScheduledVisitDto> scheduledVisits
) {

    public static RequestDetailedDto from(Request request, List<ScheduleSlot> workSlots) {
        return new RequestDetailedDto(
                request.getId(),
                request.getFullAddress(),
                request.getLatitude(),
                request.getLongitude(),
                Optional.ofNullable(request.getStatus()).map(DictionaryDto::from).orElse(null),
                request.isUrgent(),
                request.getDeadline(),
                RegionDto.from(request.getRegion()),
                JobTypeDto.from(request.getJobType()),
                Optional.ofNullable(request.getRequiredVehicle()).map(DictionaryDto::from).orElse(null),
                Optional.ofNullable(request.getRequiredEquipment())
                        .map(Equipment::getDisplayName)
                        .stream()
                        .toList(),
                request.getStartTimeWindow(),
                request.getEndTimeWindow(),
                Optional.ofNullable(request.getAssignedCrew()).map(CrewDto::from).orElse(null),
                request.getAssignedAt(),
                request.getCreatedAt(),
                request.getCompletedAt(),
                request.getCanceledAt(),
                workSlots.stream().map(ScheduledVisitDto::from).toList()
        );
    }

    public record JobTypeDto(
            String keyword,
            String displayName,
            int jobTimeSpanMinutes,
            int maxTravelTimeSpanMinutes,
            int totalTimeSpanMinutes,
            List<DictionaryDto> requiredSkills
    ) {

        public static JobTypeDto from(JobType jobType) {
            return new JobTypeDto(
                    jobType.getKeyword().name(),
                    jobType.getDisplayName(),
                    jobType.getJobTimeSpanMinutes(),
                    jobType.getMaxTravelTimeSpanMinutes(),
                    jobType.getTotalTimeSpanMinutes(),
                    Optional.ofNullable(jobType.getRequiredSkills())
                            .orElse(List.of())
                            .stream()
                            .map(DictionaryDto::from)
                            .toList()
            );
        }
    }

    public record CrewDto(
            int id,
            String name,
            LocalTime shiftStartTime,
            LocalTime shiftEndTime,
            DictionaryDto vehicle,
            List<DictionaryDto> skills
    ) {

        public static CrewDto from(EngineerCrew crew) {
            return new CrewDto(
                    crew.getId(),
                    crew.getName(),
                    crew.getShiftStartTime(),
                    crew.getShiftEndTime(),
                    Optional.ofNullable(crew.getVehicle()).map(DictionaryDto::from).orElse(null),
                    Optional.ofNullable(crew.getSkills())
                            .orElse(List.of())
                            .stream()
                            .map(DictionaryDto::from)
                            .toList()
            );
        }
    }

    /** The request's work slot in one of the schedules */
    public record ScheduledVisitDto(
            int scheduleId,
            LocalDate targetDate,
            DictionaryDto scheduleStatus,
            LocalTime arrivalTime,
            LocalTime startTime,
            LocalTime endTime,
            LocalDateTime finishedAt
    ) {

        public static ScheduledVisitDto from(ScheduleSlot slot) {
            Schedule schedule = slot.getSchedule();

            return new ScheduledVisitDto(
                    schedule.getId(),
                    schedule.getTargetDate(),
                    DictionaryDto.from(schedule.getStatus()),
                    slot.getArrivalTime(),
                    slot.getStartTimeWindow(),
                    slot.getEndTimeWindow(),
                    slot.getFinishedAt()
            );
        }
    }
}
