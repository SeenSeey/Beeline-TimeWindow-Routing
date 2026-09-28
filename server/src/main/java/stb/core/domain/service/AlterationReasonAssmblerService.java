package stb.core.domain.service;

import org.springframework.stereotype.Service;
import stb.core.domain.dto.ScheduleHistoryResult;
import stb.core.domain.model.AlterationReason;
import stb.core.domain.model.Request;
import stb.core.domain.model.ScheduleSlot;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
public class AlterationReasonAssmblerService {

    private static final Duration NOTICEABLE_START_TIME_SHIFT = Duration.ofMinutes(10);

    public List<AlterationReason> assembleReasons(
            ScheduleHistoryResult result,
            Function<ScheduleSlot, AlterationReason> assignedHandler,
            Function<ScheduleSlot, AlterationReason> reassignedHandler,
            Function<ScheduleHistoryResult.StartTimeChange, AlterationReason> startTimeChangeHandler,
            Function<Request, AlterationReason> unassignedHandler
    ) {
        return Stream.of(
                        result.assigned().stream().map(assignedHandler).toList(),
                        result.reassigned().stream().map(reassignedHandler).toList(),
                        result.startTimeChanged().stream().filter(this::isNoticeable).map(startTimeChangeHandler).toList(),
                        result.unassigned().stream().map(unassignedHandler).toList(),
                        result.notPlaced().stream().map(request -> AlterationReason.notPlaced(result.schedule(), request, result.engineerCrews())).toList()
                )
                .flatMap(Collection::stream)
                .toList();
    }

    private boolean isNoticeable(ScheduleHistoryResult.StartTimeChange change) {
        return Duration.between(change.previousStartTime(), change.slot().getStartTimeWindow())
                .abs()
                .compareTo(NOTICEABLE_START_TIME_SHIFT) > 0;
    }
}
