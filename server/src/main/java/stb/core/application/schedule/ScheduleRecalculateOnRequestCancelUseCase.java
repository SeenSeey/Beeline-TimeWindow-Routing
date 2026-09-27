package stb.core.application.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.exception.RequestNotFoundException;
import stb.core.domain.model.*;
import stb.core.domain.repository.*;
import stb.core.domain.service.ScheduleRecalculationService;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleRecalculateOnRequestCancelUseCase {

    private final RequestRepository requestRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final ScheduleRecalculationService scheduleRecalculationService;

    @Transactional
    public void execute(int requestId) {
        LocalDate targetDate = LocalDate.now();

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

        scheduleRecalculationService.recalculateSchedule(region, targetDate);

        log.info("Расписание для региона {} пересчитано в связи с отменой заявки {}", region.getDisplayName(), canceledRequest.getId());
    }
}
