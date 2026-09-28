package stb.core.domain.service;

import org.springframework.stereotype.Component;
import stb.core.domain.dto.ScheduleHistoryResult;
import stb.core.domain.model.RequestActionAudit;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Component
public class RequestActionAuditService {

    public List<RequestActionAudit> createFrom(ScheduleHistoryResult scheduleHistoryResult) {
        List<RequestActionAudit> addedToScheduleAudit = scheduleHistoryResult.assigned()
                .stream()
                .map(slot -> RequestActionAudit.assigned(slot.getRequest(), slot.getEngineerCrew(), slot.getStartTimeWindow(), slot.getEndTimeWindow()))
                .toList();

        List<RequestActionAudit> reassignedInSchedule = scheduleHistoryResult.reassigned()
                .stream()
                .map(slot -> RequestActionAudit.reassigned(slot.getRequest(), slot.getEngineerCrew(), slot.getStartTimeWindow(), slot.getEndTimeWindow()))
                .toList();

        List<RequestActionAudit> movedInSchedule = scheduleHistoryResult.startTimeChanged()
                .stream()
                .map(ScheduleHistoryResult.StartTimeChange::slot)
                .map(slot -> RequestActionAudit.rescheduled(slot.getRequest(), slot.getStartTimeWindow(), slot.getEndTimeWindow()))
                .toList();

        List<RequestActionAudit> removedFromSchedule = scheduleHistoryResult.unassigned()
                .stream()
                .map(RequestActionAudit::removedFromSchedule)
                .toList();

        return Stream.of(addedToScheduleAudit, reassignedInSchedule, removedFromSchedule, movedInSchedule).flatMap(Collection::stream).toList();
    }
}
