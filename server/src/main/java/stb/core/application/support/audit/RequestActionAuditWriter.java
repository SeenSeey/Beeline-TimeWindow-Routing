package stb.core.application.support.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stb.core.domain.model.RequestActionAudit;
import stb.core.domain.repository.RequestActionAuditRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RequestActionAuditWriter {

    private final RequestActionAuditRepository repository;

    public void save(RequestActionAudit audit) {
        repository.save(audit);
    }

    public void saveAll(List<RequestActionAudit> audits) {
        if (audits.isEmpty()) {
            return;
        }
        repository.saveAll(audits);
    }
}
