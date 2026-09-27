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
                                  List<UnassignedRequestDto> unassignedRequests,
                                  List<SlotAlterationReasonDto> alterationReasons) {

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
                unassignedRequests,
                List.of()
        );
    }

    public static ScheduleTimelineDto from(Schedule schedule,
                                           List<UnassignedRequestDto> unassignedRequests,
                                           List<EngineerCrew> regionCrews) {
        return from(schedule, unassignedRequests, regionCrews, Set.of(), List.of());
    }

    public static ScheduleTimelineDto from(Schedule schedule,
                                           List<UnassignedRequestDto> unassignedRequests,
                                           List<EngineerCrew> regionCrews,
                                           Set<Integer> offShiftCrewIds) {
        return from(schedule, unassignedRequests, regionCrews, offShiftCrewIds, List.of());
    }

    /**
     * The schedule's rows plus a row without slots for every other crew of the region;
     * crews in {@code offShiftCrewIds} are marked as taken off shift for the schedule's date.
     */
    public static ScheduleTimelineDto from(Schedule schedule,
                                           List<UnassignedRequestDto> unassignedRequests,
                                           List<EngineerCrew> regionCrews,
                                           Set<Integer> offShiftCrewIds,
                                           List<SlotAlterationReasonDto> alterationReasons) {
        List<EngineerCrewRow> rows = new ArrayList<>(EngineerCrewRow.listFrom(schedule.getSlots())
                .stream()
                .map(row -> row.withOffShift(offShiftCrewIds.contains(row.id())))
                .toList());
        Set<Integer> crewsWithSlots = rows.stream().map(EngineerCrewRow::id).collect(Collectors.toSet());
        regionCrews.stream()
                .filter(crew -> !crewsWithSlots.contains(crew.getId()))
                .forEach(crew -> rows.add(EngineerCrewRow.of(crew, List.of(), offShiftCrewIds.contains(crew.getId()))));

        return new ScheduleTimelineDto(
                schedule.getTargetDate(),
                schedule.getCreatedAt(),
                DictionaryDto.from(schedule.getStatus()),
                RegionDto.from(schedule.getRegion()),
                rows,
                unassignedRequests,
                alterationReasons
        );
    }

    public record EngineerCrewRow(int id,
                                  String name,
                                  LocalTime shiftStartTime,
                                  LocalTime shiftEndTime,
                                  DictionaryDto vehicle,
                                  List<DictionaryDto> skills,
                                  List<EquipmentDto> equipment,
                                  List<TimelineSlotDto> slots,
                                  boolean offShift) {

        public EngineerCrewRow(int id,
                               String name,
                               LocalTime shiftStartTime,
                               LocalTime shiftEndTime,
                               DictionaryDto vehicle,
                               List<DictionaryDto> skills,
                               List<EquipmentDto> equipment,
                               List<TimelineSlotDto> slots) {
            this(id, name, shiftStartTime, shiftEndTime, vehicle, skills, equipment, slots, false);
        }

        public EngineerCrewRow withOffShift(boolean offShift) {
            return new EngineerCrewRow(id, name, shiftStartTime, shiftEndTime, vehicle, skills, equipment, slots, offShift);
        }

        public static List<EngineerCrewRow> listFrom(List<ScheduleSlot> slots) {
            if (slots.isEmpty()) {
                return Collections.emptyList();
            }

            Map<Integer, List<ScheduleSlot>> slotsByEngineerId = slots
                    .stream()
                    .filter(slot -> slot.getEngineerCrew() != null)
                    .collect(Collectors.groupingBy(
                            slot -> slot.getEngineerCrew().getId(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            return slotsByEngineerId.values()
                    .stream()
                    .map(engineerSlots -> EngineerCrewRow.from(engineerSlots.getFirst().getEngineerCrew(), engineerSlots))
                    .toList();
        }

        public static EngineerCrewRow from(EngineerCrew crew, List<ScheduleSlot> slots) {
            return of(crew, slots.stream().map(TimelineSlotDto::from).toList());
        }

        public static EngineerCrewRow of(EngineerCrew crew, List<TimelineSlotDto> slots) {
            return of(crew, slots, false);
        }

        public static EngineerCrewRow of(EngineerCrew crew, List<TimelineSlotDto> slots, boolean offShift) {
            return new EngineerCrewRow(
                    crew.getId(),
                    crew.getName(),
                    crew.getShiftStartTime(),
                    crew.getShiftEndTime(),
                    Optional.ofNullable(crew.getVehicle()).map(DictionaryDto::from).orElse(null),
                    Optional.ofNullable(crew.getSkills()).orElse(List.of()).stream().map(DictionaryDto::from).toList(),
                    EquipmentDto.listFrom(crew.getEquipment()),
                    slots,
                    offShift
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
                                  String requiredEquipment,
                                  LocalTime arrivalTime,
                                  LocalTime startSlotTime,
                                  LocalTime endSlotTime,
                                  LocalTime requestTimeWindowStart,
                                  LocalTime requestTimeWindowEnd,
                                  LocalDateTime createdAt,
                                  LocalDateTime completedAt,
                                  LocalDateTime canceledAt) {

        public static TimelineSlotDto from(ScheduleSlot slot) {
            return of(slot.getRequest(), slot.getType(), slot.getArrivalTime(), slot.getStartTimeWindow(), slot.getEndTimeWindow());
        }

        public static TimelineSlotDto of(Request request,
                                         ScheduleSlot.SlotType type,
                                         LocalTime arrivalTime,
                                         LocalTime startTime,
                                         LocalTime endTime) {
            return new TimelineSlotDto(
                    request.getId(),
                    request.getFullAddress(),
                    request.getLatitude(),
                    request.getLongitude(),
                    type.name(),
                    Optional.ofNullable(request.getStatus()).map(DictionaryDto::from).orElse(null),
                    request.getDeadline(),
                    DictionaryDto.from(request.getJobType()),
                    Optional.ofNullable(request.getRequiredVehicle()).map(DictionaryDto::from).orElse(null),
                    Optional.ofNullable(request.getRequiredEquipment()).map(Equipment::getDisplayName).orElse(null),
                    arrivalTime,
                    startTime,
                    endTime,
                    request.getStartTimeWindow(),
                    request.getEndTimeWindow(),
                    request.getCreatedAt(),
                    request.getCompletedAt(),
                    request.getCanceledAt()
            );
        }
    }
}
