package stb.core.domain.dto.explanation;

import stb.core.domain.dto.explanation.ReplanContext.TriggerType;

import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ReplanningExplanation(
        TriggerType type,
        String title,
        String summary,
        LocalTime occurredAt,
        int triggerSubjectId,
        Counters counters,
        List<Change> changes
) {

    public ReplanningExplanation {
        Objects.requireNonNull(type, "type не должен быть null");
        Objects.requireNonNull(title, "title не должен быть null");
        Objects.requireNonNull(summary, "summary не должно быть null");
        Objects.requireNonNull(occurredAt, "occurredAt не должно быть null");
        Objects.requireNonNull(counters, "counters не должны быть null");
        changes = List.copyOf(changes);
    }

    public record Counters(
            int assigned,
            int unassigned,
            int reassigned,
            int reordered,
            int timeChanged
    ) {
    }

    public record Change(
            int requestId,
            Set<RequestChange.Type> changeTypes,
            String message,
            Integer previousCrewId,
            Integer newCrewId,
            LocalTime previousStartTime,
            LocalTime newStartTime
    ) {

        public Change {
            changeTypes = Collections.unmodifiableSet(EnumSet.copyOf(changeTypes));
            Objects.requireNonNull(message, "message не должно быть null");
        }
    }
}
