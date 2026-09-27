package stb.core.application.support.audit;

import org.junit.jupiter.api.Test;
import stb.core.domain.model.Request;
import stb.core.domain.model.RequestActionAudit;
import stb.core.domain.repository.RequestActionAuditRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RequestActionAuditWriterTest {

    @Test
    void writesReadyDomainAudits() {
        RequestActionAuditRepository repository = mock(RequestActionAuditRepository.class);
        RequestActionAuditWriter writer = new RequestActionAuditWriter(repository);
        LocalDateTime timestamp = LocalDateTime.of(2026, 9, 25, 12, 30);
        List<RequestActionAudit> audits = List.of(
                RequestActionAudit.started(Request.builder().build(), timestamp),
                RequestActionAudit.completed(Request.builder().build(), timestamp));

        writer.saveAll(audits);

        verify(repository).saveAll(audits);
    }

    @Test
    void skipsEmptyBatch() {
        RequestActionAuditRepository repository = mock(RequestActionAuditRepository.class);

        new RequestActionAuditWriter(repository).saveAll(List.of());

        verifyNoInteractions(repository);
    }
}
