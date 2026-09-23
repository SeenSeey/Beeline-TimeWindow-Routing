package stb.core.domain.service.scheduler;

import stb.core.domain.service.scheduler.dto.ScheduleCalculationResult;
import stb.core.domain.service.scheduler.dto.ScheduleContext;

public interface ScheduleAssemblerDomainService {

    ScheduleCalculationResult createSchedule(ScheduleContext context);
}
