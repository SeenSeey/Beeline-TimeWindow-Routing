package stb.core.application.support.audit;

import org.springframework.stereotype.Component;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.RequestActionAudit;
import stb.core.domain.model.ScheduleSlot;
import stb.core.domain.replanning.ScheduleVersionDiff;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class RequestActionAuditFactory {

    public List<RequestActionAudit> from(ScheduleVersionDiff diff, LocalDateTime timestamp) {
        List<RequestActionAudit> audits = new ArrayList<>();
        diff.added().stream()
                .map(slot -> RequestActionAudit.assigned(slot.getRequest(), slot.getEngineerCrew(),
                        slot.getStartTimeWindow(), slot.getEndTimeWindow(), timestamp))
                .forEach(audits::add);
        diff.removed().stream()
                .map(slot -> RequestActionAudit.removedFromSchedule(slot.getRequest(), timestamp))
                .forEach(audits::add);
        diff.moved().stream()
                .map(change -> rescheduled(change, timestamp))
                .forEach(audits::add);
        return List.copyOf(audits);
    }

    private static RequestActionAudit rescheduled(ScheduleVersionDiff.MovedSlot change,
                                                  LocalDateTime timestamp) {
        ScheduleSlot previous = change.previousSlot();
        ScheduleSlot current = change.currentSlot();
        EngineerCrew newCrew = previous.getEngineerCrew().getId() == current.getEngineerCrew().getId()
                ? null
                : current.getEngineerCrew();
        return RequestActionAudit.rescheduled(current.getRequest(), newCrew,
                current.getStartTimeWindow(), current.getEndTimeWindow(), timestamp);
    }
}
