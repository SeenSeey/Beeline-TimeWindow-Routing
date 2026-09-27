package stb.core.application.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.service.AlterationReasonAssmblerService;
import stb.core.domain.service.RequestActionAuditService;
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
    private final RequestActionAuditService auditFactory;
    private final AlterationReasonRepository reasonRepository;
    private final RequestActionAuditRepository requestActionAuditRepository;
    private final AlterationReasonAssmblerService alterationReasonAssmblerService;


    @Transactional
    public void execute(int crewId) {
        EngineerCrew crew = engineerCrewRepository.findById(crewId)
                .orElseThrow(() -> new CrewNotFoundException("Бригда %s не найдена".formatted(crewId)));

        List<Request> crewRequests = requestRepository.findAllIncomingByAssignedCrewId(crewId);
        if (crewRequests.isEmpty()) {
            log.info("У бригады {} нет назначенных заявок, пересчёт расписания не требуется", crewId);
            return;
        }

        Region region = crew.getRegion();
        LocalDate targetDate = LocalDate.now();

        ScheduleHistoryResult result = scheduleRecalculationService.recalculateSchedule(region, targetDate);
        log.info("Пересчёт расписания на {} в связи со снятием бригады {} со смены", targetDate, crewId);

        // requestActionAuditRepository.saveAll(auditFactory.createFrom(result));

        List<AlterationReason> reasons = alterationReasonAssmblerService.assembleReasons(
                result,
                slot -> AlterationReason.addedDueToCrewOffShift(result.schedule(), slot.getRequest(), crew),
                slot -> AlterationReason.reassignedDueToCrewOffShift(result.schedule(), slot.getRequest(), crew),
                slot -> AlterationReason.startTimeChangedDueToCrewOffShift(result.schedule(), slot.getRequest(), crew),
                request -> AlterationReason.removedDueToCrewOffShift(result.schedule(), request, crew)
        );

        // reasonRepository.saveAll(reasons);

        log.info("Расписание для региона {} пересчитано в связи с выходом инженера со смены {}", region.getDisplayName(), crew.getName());
    }
}
