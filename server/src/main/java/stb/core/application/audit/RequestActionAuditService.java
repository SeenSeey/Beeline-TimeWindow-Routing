package stb.core.application.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Request;
import stb.core.domain.model.RequestActionAudit;
import stb.core.domain.model.Schedule;
import stb.core.domain.repository.RequestActionAuditRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RequestActionAuditService {

    private final RequestActionAuditRepository repository;

    public void saveAll(List<Entry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        repository.saveAll(entries.stream().map(this::toEntity).toList());
    }

    private RequestActionAudit toEntity(Entry entry) {
        return RequestActionAudit.builder()
                .request(entry.request())
                .schedule(entry.schedule())
                .action(entry.action())
                .assignedCrew(entry.assignedCrew())
                .description(entry.description())
                .operationId(entry.operationId())
                .occurredAt(entry.occurredAt())
                .build();
    }

    public record Entry(
            Request request,
            RequestActionAudit.Action action,
            String description,
            EngineerCrew assignedCrew,
            Schedule schedule,
            UUID operationId,
            LocalDateTime occurredAt
    ) {

        public Entry {
            Objects.requireNonNull(request, "request не должна быть null");
            Objects.requireNonNull(action, "action не должен быть null");
            Objects.requireNonNull(operationId, "operationId не должен быть null");
            Objects.requireNonNull(occurredAt, "occurredAt не должен быть null");
            if (description == null || description.isBlank()) {
                throw new IllegalArgumentException("description не должно быть пустым");
            }
        }
    }
}
