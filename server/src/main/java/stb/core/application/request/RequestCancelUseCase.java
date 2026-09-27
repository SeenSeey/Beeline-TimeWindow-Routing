package stb.core.application.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.application.support.audit.RequestActionAuditWriter;
import stb.core.domain.event.RequestCanceledEvent;
import stb.core.domain.exception.RequestNotFoundException;
import stb.core.domain.model.Request;
import stb.core.domain.model.RequestActionAudit;
import stb.core.domain.model.Schedule;
import stb.core.domain.model.ScheduleSlot;
import stb.core.domain.repository.RequestRepository;
import stb.core.domain.repository.RequestStatusRepository;
import stb.core.domain.repository.ScheduleRepository;
import stb.core.domain.repository.ScheduleSlotRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestCancelUseCase {

    private final RequestRepository requestRepository;
    private final RequestStatusRepository requestStatusRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final ScheduleRepository scheduleRepository;
    private final RequestActionAuditWriter auditWriter;

    @Transactional
    public void execute(int requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Заявка не существует"));

        request.cancel(requestStatusRepository.findAll());

        ScheduleSlot activeSlot = scheduleSlotRepository.findActiveSlotByRequestId(requestId);

        if (activeSlot != null) {
            Schedule schedule = activeSlot.getSchedule();
            schedule.markAsRecalculating();

            scheduleRepository.save(schedule);
        }

        requestRepository.save(request);
        auditWriter.save(RequestActionAudit.canceled(request, LocalDateTime.now()));

        applicationEventPublisher.publishEvent(new RequestCanceledEvent(requestId));
    }
}
