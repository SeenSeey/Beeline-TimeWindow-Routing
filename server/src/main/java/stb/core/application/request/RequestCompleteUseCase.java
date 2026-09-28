package stb.core.application.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.exception.RequestNotFoundException;
import stb.core.domain.model.Request;
import stb.core.domain.model.RequestActionAudit;
import stb.core.domain.repository.RequestActionAuditRepository;
import stb.core.domain.repository.RequestRepository;
import stb.core.domain.repository.RequestStatusRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestCompleteUseCase {

    private final RequestRepository requestRepository;
    private final RequestStatusRepository requestStatusRepository;
    private final RequestActionAuditRepository requestActionAuditRepository;

    @Transactional
    public void execute(int requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Заявка не существует"));

        request.complete(requestStatusRepository.findAll());

        requestRepository.save(request);
        requestActionAuditRepository.save(RequestActionAudit.completed(request));
    }
}
