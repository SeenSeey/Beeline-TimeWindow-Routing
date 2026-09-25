package stb.core.application.support.audit;

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

class RequestActionAuditWriterTest {

    @Test
    void writesBatchWithSharedOperationAndAllowsNullableScheduleAndCrew() {
        RequestActionAuditRepository repository = mock(RequestActionAuditRepository.class);
        RequestActionAuditWriter writer = new RequestActionAuditWriter(repository);
        UUID operationId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.of(2026, 9, 25, 12, 30);

        writer.saveAll(List.of(
                entry(Request.builder().build(), "Первая заявка изменена.", operationId, timestamp),
                entry(Request.builder().build(), "Вторая заявка изменена.", operationId, timestamp)
        ));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RequestActionAudit>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2).allSatisfy(audit -> {
            assertThat(audit.getOperationId()).isEqualTo(operationId);
            assertThat(audit.getTimestamp()).isEqualTo(timestamp);
            assertThat(audit.getSchedule()).isNull();
            assertThat(audit.getAssignedCrew()).isNull();
            assertThat(audit.getDescription()).isNotBlank();
        });
    }

    @Test
    void enforcesRequiredAuditInvariants() {
        Request request = Request.builder().build();
        UUID operationId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.now();

        assertThatThrownBy(() -> new RequestActionAuditWriter.Entry(
                null, RequestActionAudit.Action.REQUEST_UPDATED, "Описание.",
                null, null, operationId, timestamp
        )).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RequestActionAuditWriter.Entry(
                request, RequestActionAudit.Action.REQUEST_UPDATED, " ",
                null, null, operationId, timestamp
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RequestActionAuditWriter.Entry(
                request, RequestActionAudit.Action.REQUEST_UPDATED, "Описание.",
                null, null, null, timestamp
        )).isInstanceOf(NullPointerException.class);
    }

    private static RequestActionAuditWriter.Entry entry(Request request,
                                                          String description,
                                                          UUID operationId,
                                                          LocalDateTime timestamp) {
        return new RequestActionAuditWriter.Entry(
                request,
                RequestActionAudit.Action.SCHEDULE_CHANGE,
                description,
                null,
                null,
                operationId,
                timestamp
        );
    }
}
