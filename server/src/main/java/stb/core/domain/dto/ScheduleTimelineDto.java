package stb.core.domain.dto;

import stb.core.domain.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public record ScheduleTimelineDto(int id,
                                  LocalDate targetDate,
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
                schedule.getId(),
                schedule.getTargetDate(),
                schedule.getCreatedAt(),
                DictionaryDto.from(schedule.getStatus()),
                RegionDto.from(schedule.getRegion()),
                EngineerCrewRow.listFrom(schedule.getSlots(), Map.of(), Map.of()),
                unassignedRequests
        );
    }

    public static ScheduleTimelineDto from(Schedule schedule,
                                           List<UnassignedRequestDto> unassignedRequests,
                                           List<EngineerCrew> regionCrews) {

        return from(schedule, unassignedRequests, regionCrews, Set.of(), Map.of(), Map.of());
    }

    public static ScheduleTimelineDto from(Schedule schedule,
                                           List<UnassignedRequestDto> unassignedRequests,
                                           List<EngineerCrew> regionCrews,
                                           Set<Integer> offShiftCrewIds) {

        return from(schedule, unassignedRequests, regionCrews, offShiftCrewIds, Map.of(), Map.of());
    }

    public static ScheduleTimelineDto from(Schedule schedule,
                                           List<UnassignedRequestDto> unassignedRequests,
                                           List<EngineerCrew> regionCrews,
                                           Set<Integer> offShiftCrewIds,
                                           Map<Integer, AlterationReasonDto> reasonsByRequestId,
                                           Map<Integer, Double> routeDistancesByCrewId) {

        List<EngineerCrewRow> rows = new ArrayList<>(EngineerCrewRow.listFrom(schedule.getSlots(), reasonsByRequestId, routeDistancesByCrewId)
                .stream()
                .map(row -> row.withOffShift(offShiftCrewIds.contains(row.id())))
                .toList());
        Set<Integer> crewsWithSlots = rows.stream().map(EngineerCrewRow::id).collect(Collectors.toSet());
        regionCrews.stream()
                .filter(crew -> !crewsWithSlots.contains(crew.getId()))
                .forEach(crew -> rows.add(EngineerCrewRow.of(
                        crew,
                        List.of(),
                        routeDistancesByCrewId.get(crew.getId()),
                        offShiftCrewIds.contains(crew.getId())
                )));

        return new ScheduleTimelineDto(
                schedule.getId(),
                schedule.getTargetDate(),
                schedule.getCreatedAt(),
                DictionaryDto.from(schedule.getStatus()),
                RegionDto.from(schedule.getRegion()),
                rows,
                unassignedRequests
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
                                  Double routeDistanceKilometers,
                                  boolean offShift) {

        public EngineerCrewRow(int id,
                               String name,
                               LocalTime shiftStartTime,
                               LocalTime shiftEndTime,
                               DictionaryDto vehicle,
                               List<DictionaryDto> skills,
                               List<EquipmentDto> equipment,
                               Double routeDistanceKilometers,
                               List<TimelineSlotDto> slots) {

            this(id, name, shiftStartTime, shiftEndTime, vehicle, skills, equipment, slots, routeDistanceKilometers, false);
        }

        public EngineerCrewRow withOffShift(boolean offShift) {
            return new EngineerCrewRow(id, name, shiftStartTime, shiftEndTime, vehicle, skills, equipment, slots, routeDistanceKilometers, offShift);
        }

        public static List<EngineerCrewRow> listFrom(List<ScheduleSlot> slots, Map<Integer, AlterationReasonDto> reasonsByRequestId, Map<Integer, Double> routeDistancesByCrewId) {
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
                    .map(engineerSlots -> {
                        EngineerCrew crew = engineerSlots.getFirst().getEngineerCrew();
                        return EngineerCrewRow.from(crew, engineerSlots, reasonsByRequestId, routeDistancesByCrewId.get(crew.getId()));
                    })
                    .toList();
        }

        public static EngineerCrewRow from(EngineerCrew crew, List<ScheduleSlot> slots, Map<Integer, AlterationReasonDto> reasonsByRequestId, Double routeDistanceKilometers) {
            return of(crew, slots.stream()
                    .map(slot -> TimelineSlotDto.from(slot, reasonsByRequestId.get(slot.getRequest().getId()), routeDistanceKilometers))
                    .toList());
        }

        /** a row without a stored route, e.g. the edit preview's recalculated rows */
        public static EngineerCrewRow of(EngineerCrew crew, List<TimelineSlotDto> slots) {
            return of(crew, slots, null, false);
        }

        public static EngineerCrewRow of(EngineerCrew crew, List<TimelineSlotDto> slots, Double routeDistanceKilometers) {
            return of(crew, slots, routeDistanceKilometers, false);
        }

        public static EngineerCrewRow of(EngineerCrew crew,
                                         List<TimelineSlotDto> slots,
                                         Double routeDistanceKilometers,
                                         boolean offShift) {
            return new EngineerCrewRow(
                    crew.getId(),
                    crew.getName(),
                    crew.getShiftStartTime(),
                    crew.getShiftEndTime(),
                    Optional.ofNullable(crew.getVehicle()).map(DictionaryDto::from).orElse(null),
                    Optional.ofNullable(crew.getSkills()).orElse(List.of()).stream().map(DictionaryDto::from).toList(),
                    EquipmentDto.listFrom(crew.getEquipment()),
                    slots,
                    routeDistanceKilometers,
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
                                  LocalDateTime canceledAt,
                                  AlterationReasonDto alterationReason) {

        public static TimelineSlotDto from(ScheduleSlot slot, AlterationReasonDto alterationReason, Double routeDistanceKilometers) {
            return of(slot.getRequest(), slot.getType(), slot.getArrivalTime(), slot.getStartTimeWindow(), slot.getEndTimeWindow(), alterationReason);
        }

        public static TimelineSlotDto of(Request request,
                                         ScheduleSlot.SlotType type,
                                         LocalTime arrivalTime,
                                         LocalTime startTime,
                                         LocalTime endTime) {
            return of(request, type, arrivalTime, startTime, endTime, null);
        }

        public static TimelineSlotDto of(Request request,
                                         ScheduleSlot.SlotType type,
                                         LocalTime arrivalTime,
                                         LocalTime startTime,
                                         LocalTime endTime,
                                         AlterationReasonDto alterationReason) {
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
                    request.getCanceledAt(),
                    alterationReason
            );
        }
    }
}
