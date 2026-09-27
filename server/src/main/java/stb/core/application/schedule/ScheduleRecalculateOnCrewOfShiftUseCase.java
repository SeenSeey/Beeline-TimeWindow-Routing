package stb.core.application.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.application.support.audit.RequestActionAuditFactory;
import stb.core.application.support.audit.RequestActionAuditWriter;
import stb.core.application.support.reason.SlotAlterationReasonFactory;
import stb.core.domain.exception.CrewNotFoundException;
import stb.core.domain.model.*;
import stb.core.domain.replanning.AutomaticReplanContext;
import stb.core.domain.replanning.ScheduleVersionDiff;
import stb.core.domain.repository.*;
import stb.core.domain.service.ScheduleRecalculationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final RequestActionAuditWriter auditWriter;
    private final SlotAlterationReasonFactory reasonFactory;
    private final SlotAlterationReasonRepository reasonRepository;

    @Transactional
    public void execute(int crewId) {
        LocalDate targetDate = LocalDate.now();
        LocalDateTime timestamp = LocalDateTime.now();

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

        ScheduleVersionDiff diff = scheduleRecalculationService.recalculateSchedule(region, targetDate);
        auditWriter.saveAll(auditFactory.from(diff, timestamp));
        reasonRepository.saveAll(reasonFactory.from(new AutomaticReplanContext.CrewOffShift(crew), diff, timestamp));

        log.info("Расписание для региона {} пересчитано в связи с выходом инженера со смены {}", region.getDisplayName(), crew.getName());
    }
}
