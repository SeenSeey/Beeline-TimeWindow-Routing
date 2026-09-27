package stb.core.application.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.application.support.audit.RequestActionAuditFactory;
import stb.core.domain.dto.ScheduleHistoryResult;
import stb.core.domain.exception.CrewNotFoundException;
import stb.core.domain.model.*;
import stb.core.domain.repository.*;
import stb.core.domain.service.ScheduleRecalculationService;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleRecalculateOnCrewOfShiftUseCase {

    private final RequestRepository requestRepository;
    private final RequestStatusRepository requestStatusRepository;
    private final EngineerCrewRepository engineerCrewRepository;
    private final ScheduleRecalculationService scheduleRecalculationService;
    private final RequestActionAuditFactory auditFactory;
    private final AlterationReasonRepository reasonRepository;
    private final RequestActionAuditRepository requestActionAuditRepository;

    @Transactional
    public void execute(int crewId) {
        LocalDate targetDate = LocalDate.now();

        List<Request> crewRequests = requestRepository.findAllIncomingByAssignedCrewId(crewId);

        EngineerCrew crew = engineerCrewRepository.findById(crewId)
                .orElseThrow(() -> new CrewNotFoundException("Бригда %s не найдена".formatted(crewId)));

        if (crewRequests.isEmpty()) {
            log.info("У бригады {} нет назначенных заявок, пересчёт расписания не требуется", crewId);
            return;
        }

        log.info("Пересчёт расписания на {} в связи со снятием бригады {} со смены", targetDate, crewId);

        crewRequests.forEach(request -> request.unassign(requestStatusRepository.findAll()));
        requestRepository.saveAllAndFlush(crewRequests);

        Region region = crew.getRegion();

        ScheduleHistoryResult result = scheduleRecalculationService.recalculateSchedule(region, targetDate);

        requestActionAuditRepository.saveAll(auditFactory.from(result));

//        List<AlterationReason> reasons = Stream.of(
//                        result.assigned().stream().map(slot -> AlterationReason.addedDueToEmergency(result.schedule(), slot.getRequest(), emergencyRequest)).toList(),
//                        result.startTimeChanged().stream().map(slot -> AlterationReason.startTimeChangedDueToEmergency(result.schedule(), slot.getRequest(), emergencyRequest)).toList(),
//                        result.unassigned().stream().map(request -> AlterationReason.removedDueToEmergency(result.schedule(), request, emergencyRequest)).toList()
//                )
//                .flatMap(Collection::stream)
//                .toList();

        reasonRepository.saveAll(reasons);

        log.info("Расписание для региона {} пересчитано в связи с выходом инженера со смены {}", region.getDisplayName(), crew.getName());
    }
}
