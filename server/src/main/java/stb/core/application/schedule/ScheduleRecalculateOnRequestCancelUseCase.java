package stb.core.application.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.service.AlterationReasonAssmblerService;
import stb.core.domain.service.RequestActionAuditService;
import stb.core.domain.dto.ScheduleHistoryResult;
import stb.core.domain.exception.RequestNotFoundException;
import stb.core.domain.model.*;
import stb.core.domain.repository.*;
import stb.core.domain.service.ScheduleRecalculationService;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleRecalculateOnRequestCancelUseCase {

    private final RequestRepository requestRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final AlterationReasonRepository reasonRepository;
    private final ScheduleRecalculationService scheduleRecalculationService;
    private final AlterationReasonAssmblerService alterationReasonAssmblerService;
    private final RequestActionAuditService auditFactory;
    private final RequestActionAuditRepository requestActionAuditRepository;

    @Transactional
    public void execute(int requestId) {
        Request canceledRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Заявка %s не существует".formatted(requestId)));

        Region region = canceledRequest.getRegion();
        LocalDate targetDate = LocalDate.now();

        ScheduleSlot latestSlot = scheduleSlotRepository.findCanceledSlotByRequestId(requestId).orElse(null);
        if (latestSlot == null) {
            log.info("Отменена заявка, которая не была включена в расписание, отмена перепланирования");
            return;
        }

        ScheduleHistoryResult result = scheduleRecalculationService.recalculateSchedule(region, targetDate);
        log.info("Пересчёт расписания на {} в связи с отменой заявки {}", targetDate, requestId);

        requestActionAuditRepository.saveAll(auditFactory.createFrom(result));

        List<AlterationReason> reasons = alterationReasonAssmblerService.assembleReasons(
                result,
                slot -> AlterationReason.addedAfterRequestCancellation(result.schedule(), slot.getRequest(), canceledRequest),
                slot -> AlterationReason.reassignedAfterRequestCancellation(result.schedule(), slot.getRequest(), canceledRequest),
                change -> AlterationReason.startTimeChangedAfterRequestCancellation(result.schedule(), change.slot().getRequest(), change.previousStartTime(), canceledRequest),
                request -> AlterationReason.removedAfterRequestCancellation(result.schedule(), request, canceledRequest)
        );

        reasonRepository.saveAll(reasons);

        log.info("Расписание для региона {} пересчитано в связи с отменой заявки {}", region.getDisplayName(), canceledRequest.getId());
    }
}
