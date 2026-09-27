package stb.core.application.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.application.support.audit.RequestActionAuditFactory;
import stb.core.domain.dto.ScheduleHistoryResult;
import stb.core.domain.exception.RequestNotFoundException;
import stb.core.domain.model.*;
import stb.core.domain.repository.*;
import stb.core.domain.service.AlterationReasonAssmblerService;
import stb.core.domain.service.ScheduleRecalculationService;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleEmergencyHandleUseCase {

    private final RequestRepository requestRepository;
    private final AlterationReasonRepository reasonRepository;
    private final ScheduleRecalculationService scheduleRecalculationService;
    private final AlterationReasonAssmblerService alterationReasonAssmblerService;
    private final RequestActionAuditFactory auditFactory;
    private final RequestActionAuditRepository requestActionAuditRepository;

    @Transactional
    public void execute(int requestId) {
        Request emergencyRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Заявка с ID = %s не найдена".formatted(requestId)));

        Region region = emergencyRequest.getRegion();
        LocalDate targetDate = LocalDate.now();

        ScheduleHistoryResult result = scheduleRecalculationService.recalculateSchedule(region, targetDate);

        requestActionAuditRepository.saveAll(auditFactory.from(result));

        List<AlterationReason> reasons = alterationReasonAssmblerService.assembleReasons(
                result,
                slot -> AlterationReason.addedDueToEmergency(result.schedule(), slot.getRequest(), emergencyRequest),
                slot -> AlterationReason.reassignedDueToEmergency(result.schedule(), slot.getRequest(), emergencyRequest),
                slot -> AlterationReason.startTimeChangedDueToEmergency(result.schedule(), slot.getRequest(), emergencyRequest),
                request -> AlterationReason.removedDueToEmergency(result.schedule(), request, emergencyRequest)
        );

        reasonRepository.saveAll(reasons);

        log.info("Расписание для региона {} пересчитано из-за появления срочной заявки {}", region.getDisplayName(), region.getId());
    }
}
