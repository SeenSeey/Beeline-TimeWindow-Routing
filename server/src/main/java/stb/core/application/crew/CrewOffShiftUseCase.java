package stb.core.application.crew;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.event.EngineerCrewTakenOffShiftEvent;
import stb.core.domain.exception.CrewAlreadyOffShiftException;
import stb.core.domain.exception.CrewNotFoundException;
import stb.core.domain.model.*;
import stb.core.domain.repository.*;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CrewOffShiftUseCase {

    private final EngineerCrewRepository engineerCrewRepository;
    private final EngineerCrewOffShiftRecordRepository engineerCrewOffShiftRecordRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final ScheduleRepository scheduleRepository;

    @Transactional
    public void execute(int crewId) {
        EngineerCrew crew = engineerCrewRepository.findById(crewId)
                .orElseThrow(() -> new CrewNotFoundException("Бригада с ID = %s не найдена".formatted(crewId)));

        LocalDate offShiftDate = LocalDate.now();

        if (engineerCrewOffShiftRecordRepository.findByEngineerCrewIdAndOffShiftDate(crewId, offShiftDate).isPresent()) {
            throw new CrewAlreadyOffShiftException("Бригада %s уже снята со смены %s".formatted(crew.getName(), offShiftDate));
        }

        List<ScheduleSlot> crewActiveSlots = scheduleSlotRepository.findAllIncomingSlotsByTargetDateAndCrewId(offShiftDate, crewId);

        if (!crewActiveSlots.isEmpty()) {
            Schedule schedule = crewActiveSlots.getFirst().getSchedule();
            schedule.markAsRecalculating();

            scheduleRepository.save(schedule);
        }

        engineerCrewOffShiftRecordRepository.save(new EngineerCrewOffShiftRecord(crew, offShiftDate));
        applicationEventPublisher.publishEvent(new EngineerCrewTakenOffShiftEvent(crewId));
    }
}
