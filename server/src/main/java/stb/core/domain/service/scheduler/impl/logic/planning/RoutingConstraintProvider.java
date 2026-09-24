package stb.core.domain.service.scheduler.impl.logic.planning;

import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import org.springframework.stereotype.Component;
import stb.core.domain.model.JobType;
import stb.core.domain.service.scheduler.dto.EngineerCrewDto;
import stb.core.domain.service.scheduler.dto.RequestDto;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Collections;

@Component
public class RoutingConstraintProvider implements ConstraintProvider {
    private static final int USED_ENGINEER_SOFT_WEIGHT = 1_000_000;
    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                hardRequestMustFinishWithinWorkDay(factory),
                hardArrivalMustBeInsideTimeWindow(factory),
                hardEngineerCrewDtoMustFinishWorkDay(factory),
                hardEngineerCrewDtoMustHaveRequiredSkill(factory),
                hardEngineerCrewDtoMustHaveRequiredTransport(factory),
                hardUrgentRequestsMustBePlanned(factory),

                mediumMaximizeCompletedRequests(factory),

                softMinimizeUsedEngineerCrews(factory),
                softMinimizeTravelTime(factory),
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


    private Constraint hardUrgentRequestsMustBePlanned(ConstraintFactory factory){
        return factory.forEachIncludingUnassigned(RequestDto.class)
                .filter(request -> request.isUrgent()
                && request.getEngineerCrewDto() == null)
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint(
                        "All urgent requests must be planned"
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
                        request -> request.getJobTypeKeyword() == JobType.Keyword.INSTALLATION ?10:1)
                .asConstraint("Maximize completed requests");
    }
}