package stb.core.domain.service;

import org.springframework.stereotype.Service;
import stb.core.domain.dto.ScheduleHistoryResult;
import stb.core.domain.model.AlterationReason;
import stb.core.domain.model.Request;
import stb.core.domain.model.ScheduleSlot;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
public class AlterationReasonAssmblerService {

    public List<AlterationReason> assembleReasons(
            ScheduleHistoryResult result,
            Function<ScheduleSlot, AlterationReason> assignedHandler,
            Function<ScheduleSlot, AlterationReason> reassignedHandler,
            Function<ScheduleSlot, AlterationReason> startTimeChangeHandler,
            Function<Request, AlterationReason> unassignedHandler
    ) {
        return Stream.of(
                        result.assigned().stream().map(assignedHandler).toList(),
                        result.reassigned().stream().map(reassignedHandler).toList(),
                        result.startTimeChanged().stream().map(startTimeChangeHandler).toList(),
                        result.unassigned().stream().map(unassignedHandler).toList()
                )
                .flatMap(Collection::stream)
                .toList();
    }

}
