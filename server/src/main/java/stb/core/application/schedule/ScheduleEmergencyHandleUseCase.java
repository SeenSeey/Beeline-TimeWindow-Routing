package stb.core.application.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.exception.RequestNotFoundException;
import stb.core.domain.model.*;
import stb.core.domain.repository.*;
import stb.core.domain.service.ScheduleRecalculationService;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleEmergencyHandleUseCase {

    private final RequestRepository requestRepository;
    private final ScheduleRecalculationService scheduleRecalculationService;

    @Transactional
    public void execute(int requestId) {
        Request emergencyRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Заявка с "));

        Region region = emergencyRequest.getRegion();
        LocalDate targetDate = LocalDate.now();

        scheduleRecalculationService.recalculateSchedule(region, targetDate);

        log.info("Расписание для региона {} пересчитано из-за появления срочной заявки {}", region.getDisplayName(), region.getId());
    }
}
