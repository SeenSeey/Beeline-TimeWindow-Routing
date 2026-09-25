package stb.core.infrastructure.event.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import stb.core.application.schedule.ScheduleRecalculateOnRequestCancelUseCase;
import stb.core.domain.event.RequestCanceledEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestCancelListener {

    private final ScheduleRecalculateOnRequestCancelUseCase scheduleRecalculateOnRequestCancelUseCase;

    @Async("scheduleExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(RequestCanceledEvent event) {
        log.info("Произошла отмена заявки {}", event.requestId());

        try {
            scheduleRecalculateOnRequestCancelUseCase.execute(event.requestId());
            log.info("Расписание успешно рассчитано в связи с отменой заявки {}", event.requestId());
        } catch (Exception exception) {
            log.error("Ошибка пересчёта расписания в связи с отменой заявки {}", event.requestId(), exception);
        }
    }
}
