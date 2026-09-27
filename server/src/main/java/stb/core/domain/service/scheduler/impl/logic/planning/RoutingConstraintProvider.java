package stb.core.domain.service.scheduler.impl.logic.planning;

import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import org.springframework.stereotype.Component;
import stb.core.domain.model.JobType;
import stb.core.domain.service.scheduler.dto.EngineerCrewDto;
import stb.core.domain.service.scheduler.dto.RequestDto;
import stb.core.domain.service.scheduler.dto.TravelMatrix;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class RoutingConstraintProvider implements ConstraintProvider {
    private static final int USED_ENGINEER_SOFT_WEIGHT = 1_000_000;
    private static final int URGENT_START_DELAY_SOFT_WEIGHT = 100;
    private static final int DEADLINE_WEIGHT = 1_000;

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                hardRequestMustFinishWithinWorkDay(factory),
                hardArrivalMustBeInsideTimeWindow(factory),
                hardEngineerCrewDtoMustFinishWorkDay(factory),
                hardEngineerCrewDtoMustHaveRequiredSkill(factory),
                hardEngineerCrewDtoMustHaveRequiredTransport(factory),
                hardEngineerCrewDtoMustFitEquipmentCapacity(factory),
                hardEngineerCrewDtoMustHaveRequiredEquipment(factory),

                mediumMaximizeCompletedRequests(factory),
                mediumMinimizeUnusedEngineerCrews(factory),
                mediumPrioritiseDeadlierRequests(factory),

                softMinimizeUsedEngineerCrews(factory),
                softMinimizeTravelDistance(factory),
                softMinimizeTravelTime(factory),
                softStartUrgentRequestsAsEarlyAsPossible(factory),
//                softMinimizeWaitingTime(factory)
        };
    }


    private Constraint hardRequestMustFinishWithinWorkDay(
            ConstraintFactory factory
    ) {
        return factory
                .forEach(RequestDto.class)
                .filter(request ->
                        request.getEngineerCrewDto() != null
                )
                .filter(request ->
                        request.getDepartureTime() == null || request.getDepartureTime().isAfter(request.getEngineerCrewDto().getWorkDayEnd())
                )
                .penalize(
                        HardMediumSoftScore.ONE_HARD
                )
                .asConstraint(
                        "Request must finish within engineer work day"
                );
    }


    private Constraint hardArrivalMustBeInsideTimeWindow(
            ConstraintFactory factory
    ) {
        return factory
                .forEach(RequestDto.class)
                .filter(request ->
                        request.getArrivalTime() != null
                                && request.getArrivalTime().isAfter(request.getWindowEnd())
                )
                .penalize(
                        HardMediumSoftScore.ONE_HARD
                )
                .asConstraint(
                        "request must fit into time window"
                );

    }

    private Constraint hardEngineerCrewDtoMustHaveRequiredTransport(ConstraintFactory factory) {
        return factory
                .forEach(EngineerCrewDto.class)
                .penalize(
                        HardMediumSoftScore.ONE_HARD,
                        engineerCrewDto -> (int) engineerCrewDto.getRoute().stream()
                                .filter(request -> request.getRequiredTransportTypeDto() != null)
                                .filter(request -> request.getRequiredTransportTypeDto()
                                        != engineerCrewDto.getTransportTypeDto())
                                .count()
                )
                .asConstraint("EngineerCrewDto must have required transport");
    }

    private Constraint hardEngineerCrewDtoMustFitEquipmentCapacity(ConstraintFactory factory) {
        return factory
                .forEach(EngineerCrewDto.class)
                .filter(engineerCrewDto -> engineerCrewDto.getEquipmentStock() == null)
                .penalize(
                        HardMediumSoftScore.ONE_HARD,
                        engineerCrewDto -> Math.max(0, (int) engineerCrewDto.getRoute().stream()
                                .filter(request -> request.getRequiredEquipmentId() != null)
                                .count() - engineerCrewDto.getEquipmentCapacity())
                )
                .asConstraint("EngineerCrewDto must fit equipment capacity");
    }

    private Constraint hardEngineerCrewDtoMustHaveRequiredEquipment(ConstraintFactory factory) {
        return factory
                .forEach(EngineerCrewDto.class)
                .filter(engineerCrewDto -> engineerCrewDto.getEquipmentStock() != null)
                .penalize(
                        HardMediumSoftScore.ONE_HARD,
                        RoutingConstraintProvider::missingEquipment
                )
                .asConstraint("EngineerCrewDto must have required equipment");
    }

    private static int missingEquipment(EngineerCrewDto engineerCrewDto) {
        Map<Integer, Integer> stock = new HashMap<>(engineerCrewDto.getEquipmentStock());
        int missing = 0;
        for (RequestDto request : engineerCrewDto.getRoute()) {
            Integer equipmentId = request.getRequiredEquipmentId();
            if (equipmentId == null) {
                continue;
            }
            if (stock.getOrDefault(equipmentId, 0) > 0) {
                stock.merge(equipmentId, -1, Integer::sum);
            } else {
                missing++;
            }
        }
        return missing;
    }

    private Constraint hardEngineerCrewDtoMustHaveRequiredSkill(
            ConstraintFactory factory
    ) {
        return factory
                .forEach(EngineerCrewDto.class)
                .penalize(
                        HardMediumSoftScore.ONE_HARD,
                        engineerCrewDto -> (int) engineerCrewDto.getRoute().stream()
                                .filter(request ->
                                                Collections.disjoint(engineerCrewDto.getSkills(), request.getRequiredSkills())
                                )
                                .count()
                )
                .asConstraint(
                        "EngineerCrewDto must have required skill"
                );
    }

    private Constraint hardEngineerCrewDtoMustFinishWorkDay(
            ConstraintFactory factory
    ) {
        return factory
                .forEach(EngineerCrewDto.class)
                .filter(engineerCrewDto -> {
                            if (engineerCrewDto.getRoute().isEmpty()) {
                                return false;
                            }
                            RequestDto lastRequest =
                                    engineerCrewDto.getRoute()
                                            .getLast();

                    return lastRequest.getDepartureTime() != null
                            && lastRequest.getDepartureTime()
                            .isAfter(engineerCrewDto.getWorkDayEnd());
                })
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint(
                        "EngineerCrewDto must finish before end of work day"
                );
    }

    private Constraint softMinimizeTravelTime(
            ConstraintFactory factory
    ) {
        return factory
                .forEach(RequestDto.class)
                .filter(request ->
                        request.getEngineerCrewDto() != null
                                && request.getArrivalTime() != null
                )
                .penalize(
                        HardMediumSoftScore.ONE_SOFT,
                        request -> {

                            LocalTime departureTime;

                            if (request.getPreviousRequest() == null) {
                                departureTime =
                                        request.getEngineerCrewDto()
                                                .getWorkDayStart();
                            } else {
                                departureTime =
                                        request.getPreviousRequest()
                                                .getDepartureTime();
                            }

                            if (departureTime == null) {
                                return 0L;
                            }

                            return Duration.between(
                                    departureTime,
                                    request.getArrivalTime()
                            ).toMinutes();
                        }
                )
                .asConstraint(
                        "Minimize travel time"
                );
    }
    private Constraint softStartUrgentRequestsAsEarlyAsPossible(
            ConstraintFactory factory
    ) {
        return factory
                .forEach(RequestDto.class)
                .filter(request ->
                        request.isUrgent()
                                && request.getServiceStartTime() != null
                )
                .penalize(
                        HardMediumSoftScore.ONE_SOFT,
                        request -> URGENT_START_DELAY_SOFT_WEIGHT * (int) Duration.between(
                                request.getWindowStart(),
                                request.getServiceStartTime()
                        ).toMinutes()
                )
                .asConstraint(
                        "Start urgent requests as early as possible"
                );
    }

    private Constraint softMinimizeWaitingTime(
            ConstraintFactory factory
    ) {
        return factory
                .forEach(EngineerCrewDto.class)
                .penalize(
                        HardMediumSoftScore.ONE_SOFT,
                        engineerCrewDto ->
                                engineerCrewDto.getRoute()
                                        .stream()
                                        .filter(request ->
                                                request.getArrivalTime() != null
                                                        && request.getServiceStartTime() != null
                                        )
                                        .mapToLong(request ->
                                                Duration.between(
                                                        request.getArrivalTime(),
                                                        request.getServiceStartTime()
                                                ).toMinutes()
                                        )
                                        .sum()
                )
                .asConstraint(
                        "Minimize waiting time"
                );
    }

    private Constraint softMinimizeUsedEngineerCrews(
            ConstraintFactory factory
    ) {
        return factory
                .forEach(EngineerCrewDto.class)
                .filter(engineerCrewDto ->
                        !engineerCrewDto.getRoute().isEmpty()
                )
                .penalize(
                        HardMediumSoftScore.ONE_SOFT,
                        engineerCrewDto -> USED_ENGINEER_SOFT_WEIGHT
                )
                .asConstraint(
                        "Minimize number of used engineerCrews"
                );
    }


    private Constraint mediumMaximizeCompletedRequests(ConstraintFactory factory) {  //приоритет влияет не на срочность/временное окно, а на ценность выполнения заявки.
        return factory
                .forEachIncludingUnassigned(RequestDto.class)
                .filter(request -> request.getEngineerCrewDto() == null)
                .penalize(HardMediumSoftScore.ONE_MEDIUM,
                        request -> request.isUrgent() ? 1000 : request.getJobTypeKeyword() == JobType.Keyword.INSTALLATION ?10:1)
                .asConstraint("Maximize completed requests");
    }


    private Constraint softMinimizeTravelDistance(
            ConstraintFactory factory
    ) {
        return factory
                .forEach(RequestDto.class)
                .filter(request ->
                        request.getEngineerCrewDto() != null
                                && request.getArrivalTime() != null
                )
                .join(TravelMatrix.class)
                .penalize(
                        HardMediumSoftScore.ONE_SOFT,
                        (request, travelMatrix)  -> {
                            if (request.getPreviousRequest() == null) {
                                return (long) Math.ceil(travelMatrix.distanceBetween(request.getLocation(),request.getEngineerCrewDto().getStartLocation()));
                            } else {
                                return (long) Math.ceil(travelMatrix.distanceBetween(request.getLocation(),request.getPreviousRequest().getLocation()));
                            }
                        }
                )
                .asConstraint(
                        "Minimize travel distance"
                );
    }

    private Constraint mediumMinimizeUnusedEngineerCrews(ConstraintFactory factory){
        return factory.forEach(EngineerCrewDto.class)
                .penalize(HardMediumSoftScore.ONE_MEDIUM,
                crew -> crew.isUsed()?0:5)
                .asConstraint("Minimize unused engineer crews");
    }

    private Constraint mediumPrioritiseDeadlierRequests(ConstraintFactory factory){
        return factory.forEach(RequestDto.class)
                .filter(request ->
                request.getEngineerCrewDto() != null
                        && request.getArrivalTime() != null
                )
                .penalize(HardMediumSoftScore.ONE_SOFT,
                        request -> (DEADLINE_WEIGHT *
                                 Duration.between(request.getCreatedAt(), request.getDeadline()).toMinutes()
                                / Duration.between(LocalDateTime.now(), request.getDeadline()).toMinutes()
                        )
                ).asConstraint(
                        "Prioritise deadlier requests"
                );
    }
}