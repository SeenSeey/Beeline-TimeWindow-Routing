package stb.core.application.request;

import org.junit.jupiter.api.Test;
import stb.core.domain.exception.RequestNotFoundException;
import stb.core.domain.model.RequestActionAudit;
import stb.core.domain.repository.RequestActionAuditRepository;
import stb.core.domain.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RequestHistoryFindUseCaseTest {

    @Test
    void returnsStoredDescriptionsInRepositoryTimelineOrder() {
        RequestRepository requestRepository = mock(RequestRepository.class);
        RequestActionAuditRepository auditRepository = mock(RequestActionAuditRepository.class);
        RequestHistoryFindUseCase useCase = new RequestHistoryFindUseCase(requestRepository, auditRepository);
        when(requestRepository.existsById(42)).thenReturn(true);
        when(auditRepository.findAllByRequestIdOrderByOccurredAtDescIdDesc(42)).thenReturn(List.of(
                audit("Новое объяснение.", LocalDateTime.of(2026, 9, 25, 12, 0)),
                audit("Старое объяснение.", LocalDateTime.of(2026, 9, 25, 10, 0))
        ));

        var result = useCase.execute(42);

        assertThat(result).extracting(item -> item.description())
                .containsExactly("Новое объяснение.", "Старое объяснение.");
        verify(auditRepository).findAllByRequestIdOrderByOccurredAtDescIdDesc(42);
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

    private static RequestActionAudit audit(String description, LocalDateTime occurredAt) {
        return RequestActionAudit.builder()
                .action(RequestActionAudit.Action.SCHEDULE_CHANGE)
                .description(description)
                .operationId(UUID.randomUUID())
                .occurredAt(occurredAt)
                .build();
    }
}
