package stb.core.application.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.application.support.audit.RequestActionAuditFactory;
import stb.core.application.support.audit.RequestActionAuditWriter;
import stb.core.application.support.reason.SlotAlterationReasonFactory;
import stb.core.domain.exception.RequestNotFoundException;
import stb.core.domain.model.*;
import stb.core.domain.replanning.AutomaticReplanContext;
import stb.core.domain.replanning.ScheduleVersionDiff;
import stb.core.domain.repository.*;
import stb.core.domain.service.ScheduleRecalculationService;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleEmergencyHandleUseCase {

    private final RequestRepository requestRepository;
    private final ScheduleRecalculationService scheduleRecalculationService;
    private final RequestActionAuditFactory auditFactory;
    private final RequestActionAuditWriter auditWriter;
    private final SlotAlterationReasonFactory reasonFactory;
    private final SlotAlterationReasonRepository reasonRepository;

    @Transactional
    public void execute(int requestId) {
        Request emergencyRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Заявка с "));

        Region region = emergencyRequest.getRegion();
        LocalDate targetDate = LocalDate.now();
        LocalDateTime timestamp = LocalDateTime.now();

        ScheduleVersionDiff diff = scheduleRecalculationService.recalculateSchedule(region, targetDate);
        auditWriter.saveAll(auditFactory.from(diff, timestamp));
        reasonRepository.saveAll(reasonFactory.from(new AutomaticReplanContext.HigherPriorityRequest(emergencyRequest), diff, timestamp));

        log.info("Расписание для региона {} пересчитано из-за появления срочной заявки {}", region.getDisplayName(), region.getId());
    }
}
