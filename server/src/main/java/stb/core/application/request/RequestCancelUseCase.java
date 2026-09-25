package stb.core.application.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.event.RequestCanceledEvent;
import stb.core.domain.exception.RequestNotFoundException;
import stb.core.domain.model.Request;
import stb.core.domain.repository.RequestRepository;
import stb.core.domain.repository.RequestStatusRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestCancelUseCase {

    private final RequestRepository requestRepository;
    private final RequestStatusRepository requestStatusRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void execute(int requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Заявка не существует"));

        request.cancel(requestStatusRepository.findAll());

        requestRepository.save(request);

        applicationEventPublisher.publishEvent(new RequestCanceledEvent(requestId));
    }
}
