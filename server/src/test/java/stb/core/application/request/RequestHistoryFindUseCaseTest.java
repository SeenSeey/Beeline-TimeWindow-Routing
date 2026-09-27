package stb.core.application.request;

import org.junit.jupiter.api.Test;
import stb.core.domain.exception.RequestNotFoundException;
import stb.core.domain.model.Request;
import stb.core.domain.model.RequestActionAudit;
import stb.core.domain.repository.RequestActionAuditRepository;
import stb.core.domain.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RequestHistoryFindUseCaseTest {

    @Test
    void returnsFixedActionDescriptionsInRepositoryTimelineOrder() {
        RequestRepository requestRepository = mock(RequestRepository.class);
        RequestActionAuditRepository auditRepository = mock(RequestActionAuditRepository.class);
        RequestHistoryFindUseCase useCase = new RequestHistoryFindUseCase(requestRepository, auditRepository);
        when(requestRepository.existsById(42)).thenReturn(true);
        when(auditRepository.findAllByRequestIdOrderByTimestampDescIdDesc(42)).thenReturn(List.of(
                RequestActionAudit.completed(Request.builder().build(), LocalDateTime.of(2026, 9, 25, 12, 0)),
                RequestActionAudit.started(Request.builder().build(), LocalDateTime.of(2026, 9, 25, 10, 0))
        ));

        var result = useCase.execute(42);

        assertThat(result).extracting(item -> item.description())
                .containsExactly("Выполнена", "Взята в работу");
        verify(auditRepository).findAllByRequestIdOrderByTimestampDescIdDesc(42);
    }

    @Test
    void rejectsUnknownRequestWithoutReadingAudit() {
        RequestRepository requestRepository = mock(RequestRepository.class);
        RequestActionAuditRepository auditRepository = mock(RequestActionAuditRepository.class);
        RequestHistoryFindUseCase useCase = new RequestHistoryFindUseCase(requestRepository, auditRepository);
        when(requestRepository.existsById(42)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(42)).isInstanceOf(RequestNotFoundException.class);

        verifyNoInteractions(auditRepository);
    }
}
