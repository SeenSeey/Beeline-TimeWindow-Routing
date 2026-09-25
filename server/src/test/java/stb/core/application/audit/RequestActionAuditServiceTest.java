package stb.core.application.audit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import stb.core.domain.model.Request;
import stb.core.domain.model.RequestActionAudit;
import stb.core.domain.repository.RequestActionAuditRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RequestActionAuditServiceTest {

    @Test
    void writesBatchWithSharedOperationAndAllowsNullableScheduleAndCrew() {
        RequestActionAuditRepository repository = mock(RequestActionAuditRepository.class);
        RequestActionAuditService service = new RequestActionAuditService(repository);
        UUID operationId = UUID.randomUUID();
        LocalDateTime occurredAt = LocalDateTime.of(2026, 9, 25, 12, 30);

        service.saveAll(List.of(
                entry(Request.builder().build(), "Первая заявка изменена.", operationId, occurredAt),
                entry(Request.builder().build(), "Вторая заявка изменена.", operationId, occurredAt)
        ));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RequestActionAudit>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2).allSatisfy(audit -> {
            assertThat(audit.getOperationId()).isEqualTo(operationId);
            assertThat(audit.getOccurredAt()).isEqualTo(occurredAt);
            assertThat(audit.getSchedule()).isNull();
            assertThat(audit.getAssignedCrew()).isNull();
            assertThat(audit.getDescription()).isNotBlank();
        });
    }

    @Test
    void enforcesRequiredAuditInvariants() {
        Request request = Request.builder().build();
        UUID operationId = UUID.randomUUID();
        LocalDateTime occurredAt = LocalDateTime.now();

        assertThatThrownBy(() -> new RequestActionAuditService.Entry(
                null, RequestActionAudit.Action.REQUEST_UPDATED, "Описание.",
                null, null, operationId, occurredAt
        )).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RequestActionAuditService.Entry(
                request, RequestActionAudit.Action.REQUEST_UPDATED, " ",
                null, null, operationId, occurredAt
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RequestActionAuditService.Entry(
                request, RequestActionAudit.Action.REQUEST_UPDATED, "Описание.",
                null, null, null, occurredAt
        )).isInstanceOf(NullPointerException.class);
    }

    private static RequestActionAuditService.Entry entry(Request request,
                                                          String description,
                                                          UUID operationId,
                                                          LocalDateTime occurredAt) {
        return new RequestActionAuditService.Entry(
                request,
                RequestActionAudit.Action.SCHEDULE_CHANGE,
                description,
                null,
                null,
                operationId,
                occurredAt
        );
    }
}
