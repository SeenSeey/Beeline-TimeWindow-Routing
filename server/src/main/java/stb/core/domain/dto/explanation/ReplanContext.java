package stb.core.domain.dto.explanation;

import java.time.LocalTime;
import java.util.Objects;

public record ReplanContext(
        TriggerType triggerType,
        LocalTime occurredAt,
        int triggerSubjectId,
        int previousPlanId,
        int newPlanId
) {

    public ReplanContext {
        Objects.requireNonNull(triggerType, "triggerType не должен быть null");
        Objects.requireNonNull(occurredAt, "occurredAt не должно быть null");
    }

    public enum TriggerType {
        EMERGENCY_REQUEST_CREATED,
        REQUEST_CANCELLED,
        CREW_BECAME_UNAVAILABLE
    }
}
