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
public class ScheduleRecalculateOnRequestCancelUseCase {

    private final RequestRepository requestRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final ScheduleRecalculationService scheduleRecalculationService;
    private final RequestActionAuditFactory auditFactory;
    private final RequestActionAuditWriter auditWriter;
    private final AlterationReasonFactory reasonFactory;
    private final AlterationReasonRepository reasonRepository;

    @Transactional
    public void execute(int requestId) {
        LocalDate targetDate = LocalDate.now();
        LocalDateTime timestamp = LocalDateTime.now();

        log.info("Пересчёт расписания на {} в связи с отменой заявки {}", targetDate, requestId);

        Request canceledRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Заявка %s не существует".formatted(requestId)));

        Region region = canceledRequest.getRegion();
        ScheduleSlot latestSlot = scheduleSlotRepository.findCanceledSlotByRequestId(requestId)
                .orElse(null);

        if (latestSlot == null) {
            log.info("Отменена заявка, которая не была включена в расписание, отмена перепланирования");
            return;
        }

        ScheduleRecalculationService.Recalculation recalculation =
                scheduleRecalculationService.recalculateSchedule(region, targetDate, timestamp);
        auditWriter.saveAll(auditFactory.from(recalculation.diff(), timestamp));
        List<AlterationReason> reasons = new ArrayList<>(reasonFactory.from(
                new AutomaticReplanContext.RequestCancellation(canceledRequest), recalculation.schedule(), recalculation.diff(), timestamp));
        reasons.addAll(recalculation.unscheduledReasons());
        reasonRepository.saveAll(reasons);

        log.info("Расписание для региона {} пересчитано в связи с отменой заявки {}", region.getDisplayName(), canceledRequest.getId());
    }
}
