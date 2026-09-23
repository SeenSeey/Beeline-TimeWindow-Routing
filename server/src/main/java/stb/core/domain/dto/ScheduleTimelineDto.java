package stb.core.domain.dto;

import stb.core.domain.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public record ScheduleTimelineDto(LocalDate targetDate,
                                  LocalDateTime createdAt,
                                  DictionaryDto status,
                                  RegionDto region,
                                  List<EngineerCrewRow> engineers,
                                  List<UnassignedRequestDto> unassignedRequests) {

    public static ScheduleTimelineDto from(Schedule schedule) {
        return from(schedule, List.of());
    }

    public static ScheduleTimelineDto from(Schedule schedule, List<UnassignedRequestDto> unassignedRequests) {
        return new ScheduleTimelineDto(
                schedule.getTargetDate(),
                schedule.getCreatedAt(),
                DictionaryDto.from(schedule.getStatus()),
                RegionDto.from(schedule.getRegion()),
                EngineerCrewRow.listFrom(schedule.getSlots()),
                unassignedRequests
        );
    }

    public record EngineerCrewRow(int id,
                                  String name,
                                  LocalTime shiftStartTime,
                                  LocalTime shiftEndTime,
                                  List<TimelineSlotDto> slots) {

        public static List<EngineerCrewRow> listFrom(List<ScheduleSlot> slots) {
            if (slots.isEmpty()) {
                return Collections.emptyList();
            }

            Map<Integer, List<ScheduleSlot>> slotsByEngineerId = slots
                    .stream()
                    .filter(slot -> slot.getRequest().getAssignedCrew() != null)
                    .collect(Collectors.groupingBy(
                            slot -> slot.getRequest().getAssignedCrew().getId(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            return slotsByEngineerId.values()
                    .stream()
                    .map(engineerSlots -> EngineerCrewRow.from(engineerSlots.getFirst().getRequest().getAssignedCrew(), engineerSlots))
                    .toList();
        }

        public static EngineerCrewRow from(EngineerCrew crew, List<ScheduleSlot> slots) {
            return new EngineerCrewRow(
                    crew.getId(),
                    crew.getName(),
                    crew.getShiftStartTime(),
                    crew.getShiftEndTime(),
                    slots.stream().map(TimelineSlotDto::from).toList()
            );
        }
    }

    public record TimelineSlotDto(int requestId,
                                  String fullAddress,
                                  double latitude,
                                  double longitude,
                                  String slotType,
                                  DictionaryDto status,
                                  LocalDateTime deadline,
                                  DictionaryDto jobType,
                                  DictionaryDto requiredVehicle,
                                  LocalTime arrivalTime,
                                  LocalTime startSlotTime,
                                  LocalTime endSlotTime,
                                  LocalTime requestTimeWindowStart,
                                  LocalTime requestTimeWindowEnd,
                                  LocalDateTime createdAt,
                                  LocalDateTime completedAt,
                                  LocalDateTime canceledAt) {

        public static TimelineSlotDto from(ScheduleSlot slot) {
            return new TimelineSlotDto(
                    slot.getRequest().getId(),
                    slot.getRequest().getFullAddress(),
                    slot.getRequest().getLatitude(),
                    slot.getRequest().getLongitude(),
                    slot.getType().name(),
                    Optional.ofNullable(slot.getRequest().getStatus()).map(DictionaryDto::from).orElse(null),
                    slot.getRequest().getDeadline(),
                    DictionaryDto.from(slot.getRequest().getJobType()),
                    Optional.ofNullable(slot.getRequest().getRequiredVehicle()).map(DictionaryDto::from).orElse(null),
                    slot.getArrivalTime(),
                    slot.getStartTimeWindow(),
                    slot.getEndTimeWindow(),
                    slot.getRequest().getStartTimeWindow(),
                    slot.getRequest().getEndTimeWindow(),
                    slot.getRequest().getCreatedAt(),
                    slot.getRequest().getCompletedAt(),
                    slot.getRequest().getCanceledAt()
            );
        }
    }
}
