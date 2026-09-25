package stb.core.application.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.dto.RequestHistoryItemDto;
import stb.core.domain.exception.RequestNotFoundException;
import stb.core.domain.repository.RequestActionAuditRepository;
import stb.core.domain.repository.RequestRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RequestHistoryFindUseCase {

    private final RequestRepository requestRepository;
    private final RequestActionAuditRepository auditRepository;

    @Transactional(readOnly = true)
    public List<RequestHistoryItemDto> execute(int requestId) {
        if (!requestRepository.existsById(requestId)) {
            throw new RequestNotFoundException("Заявка №%d не найдена".formatted(requestId));
        }
        return auditRepository.findAllByRequestIdOrderByOccurredAtDescIdDesc(requestId).stream()
                .map(RequestHistoryItemDto::from)
                .toList();
    }
}
