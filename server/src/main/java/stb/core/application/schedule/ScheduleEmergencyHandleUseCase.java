package stb.core.application.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.application.support.audit.RequestActionAuditFactory;
import stb.core.application.support.audit.RequestActionAuditWriter;
import stb.core.application.support.reason.AlterationReasonFactory;
import stb.core.domain.exception.RequestNotFoundException;
import stb.core.domain.model.*;
import stb.core.domain.replanning.AutomaticReplanContext;
import stb.core.domain.repository.*;
import stb.core.domain.service.ScheduleRecalculationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleEmergencyHandleUseCase {

    private final RequestRepository requestRepository;
    private final ScheduleRecalculationService scheduleRecalculationService;
    private final RequestActionAuditFactory auditFactory;
    private final RequestActionAuditWriter auditWriter;
    private final AlterationReasonFactory reasonFactory;
    private final AlterationReasonRepository reasonRepository;

    @Transactional
    public void execute(int requestId) {
        Request emergencyRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Заявка с "));

        Region region = emergencyRequest.getRegion();
        LocalDate targetDate = LocalDate.now();
        LocalDateTime timestamp = LocalDateTime.now();

        ScheduleRecalculationService.Recalculation recalculation =
                scheduleRecalculationService.recalculateSchedule(region, targetDate, timestamp);
        auditWriter.saveAll(auditFactory.from(recalculation.diff(), timestamp));
        List<AlterationReason> reasons = new ArrayList<>(reasonFactory.from(
                new AutomaticReplanContext.HigherPriorityRequest(emergencyRequest), recalculation.schedule(), recalculation.diff(), timestamp));
        reasons.addAll(recalculation.unscheduledReasons());
        reasonRepository.saveAll(reasons);

        log.info("Расписание для региона {} пересчитано из-за появления срочной заявки {}", region.getDisplayName(), region.getId());
    }
}
