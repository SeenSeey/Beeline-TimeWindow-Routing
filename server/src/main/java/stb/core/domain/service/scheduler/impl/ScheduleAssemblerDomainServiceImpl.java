package stb.core.domain.service.scheduler.impl;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Request;
import stb.core.domain.repository.projection.CrewLatestSlot;
import stb.core.domain.service.scheduler.ScheduleAssemblerDomainService;
import stb.core.domain.service.scheduler.dto.EngineerCrewDto;
import stb.core.domain.service.scheduler.dto.RequestDto;
import stb.core.domain.service.scheduler.dto.ScheduleCalculationResult;
import stb.core.domain.service.scheduler.dto.ScheduleContext;
import stb.core.domain.service.scheduler.impl.logic.planning.EngineerRoutingPlan;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleAssemblerDomainServiceImpl implements ScheduleAssemblerDomainService {

    private final SchedulerMapper schedulerMapper;
    private final SolverManager<EngineerRoutingPlan> solverManager;

    @Override
    public ScheduleCalculationResult createSchedule(ScheduleContext context) {
        List<EngineerCrewDto> engineerCrewDtoList = new ArrayList<>();
        Map<Integer, CrewLatestSlot> crewLatestSlots = context.getCrewsLatestSlots() != null
                ? context.getCrewsLatestSlots().stream().collect(Collectors.toMap(CrewLatestSlot::getCrewId, Function.identity()))
                : new HashMap<>();

        for (EngineerCrew engineerCrew : context.getEngineerCrews()){
            engineerCrewDtoList.add(schedulerMapper.toEngineerCrewDto(
                    engineerCrew,
                    crewLatestSlots,
                    context.getCrewsUsedInSchedule(),
                    context.isReplanning())
            );
        }

        List<RequestDto> requestDtoList = new ArrayList<>();

        for (Request request : context.getRequests()){
            requestDtoList.add(schedulerMapper.toRequestDto(request));
        }

        EngineerRoutingPlan problem = new EngineerRoutingPlan(context.getTravelMatrix(),
                context.getSpeedProfile(),
                engineerCrewDtoList,
                requestDtoList);

        UUID problemId = UUID.randomUUID();

        SolverJob<EngineerRoutingPlan> solverRequest = solverManager
                .solveBuilder()
                .withProblemId(problemId)
                .withProblem(problem)
                .run();


        try {
            EngineerRoutingPlan result = solverRequest.getFinalBestSolution();
            log.info("Расчёт расписания завершён, score: {}", result.getScore());
            return schedulerMapper.toSchedulerResult(result);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Solving was interrupted.", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Solving failed.", exception);
        }
    }
}
