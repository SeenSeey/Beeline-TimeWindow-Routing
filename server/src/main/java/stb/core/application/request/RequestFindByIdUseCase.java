package stb.core.application.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.dto.RequestDetailedDto;
import stb.core.domain.exception.RequestNotFoundException;
import stb.core.domain.model.Request;
import stb.core.domain.model.ScheduleSlot;
import stb.core.domain.repository.RequestActionAuditRepository;
import stb.core.domain.repository.RequestRepository;
import stb.core.domain.repository.ScheduleSlotRepository;

@Component
@RequiredArgsConstructor
public class RequestFindByIdUseCase {

    private final RequestRepository requestRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final RequestActionAuditRepository auditRepository;

    // Transactional: the DTO reads lazy associations (job type skills, crew vehicle/skills/equipment)
    @Transactional(readOnly = true)
    public RequestDetailedDto execute(int requestId) {
        Request request = requestRepository
                .findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Заявка №%d не найдена".formatted(requestId)));

        return RequestDetailedDto.from(
                request,
                scheduleSlotRepository.findAllByRequestIdAndType(requestId, ScheduleSlot.SlotType.REQUEST),
                auditRepository.findAllByRequestIdOrderByTimestampDescIdDesc(requestId)
        );
    }
}
