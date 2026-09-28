package stb.core.application.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.port.route.RouteStorage;
import stb.core.domain.service.RequestActionAuditService;
import stb.core.domain.dto.ScheduleHistoryResult;
import stb.core.domain.exception.RequestNotFoundException;
import stb.core.domain.model.*;
import stb.core.domain.repository.*;
import stb.core.domain.service.AlterationReasonAssmblerService;
import stb.core.domain.service.ScheduleRecalculationService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleEmergencyHandleUseCase {

    private final RequestRepository requestRepository;
    private final AlterationReasonRepository reasonRepository;
    private final ScheduleRecalculationService scheduleRecalculationService;
    private final AlterationReasonAssmblerService alterationReasonAssmblerService;
    private final RequestActionAuditRepository requestActionAuditRepository;
    private final RequestActionAuditService auditFactory;
    private final RouteStorage routeStorage;

    @Transactional
    public void execute(int requestId) {
        Request emergencyRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Заявка с ID = %s не найдена".formatted(requestId)));

        Region region = emergencyRequest.getRegion();
        LocalDate targetDate = LocalDate.now();

        routeStorage.clearAll();

        ScheduleHistoryResult result = scheduleRecalculationService.recalculateSchedule(region, targetDate, Collections.emptyList());

        requestActionAuditRepository.saveAll(auditFactory.createFrom(result));

        List<AlterationReason> reasons = alterationReasonAssmblerService.assembleReasons(
                result,
                slot -> AlterationReason.addedDueToEmergency(result.schedule(), slot.getRequest(), emergencyRequest),
                slot -> AlterationReason.reassignedDueToEmergency(result.schedule(), slot.getRequest(), emergencyRequest),
                change -> AlterationReason.startTimeChangedDueToEmergency(result.schedule(), change.slot().getRequest(), change.previousStartTime(), emergencyRequest),
                request -> AlterationReason.removedDueToEmergency(result.schedule(), request, emergencyRequest)
        );

        reasonRepository.saveAll(reasons);

        log.info("Расписание для региона {} пересчитано из-за появления срочной заявки {}", region.getDisplayName(), region.getId());
    }
}
