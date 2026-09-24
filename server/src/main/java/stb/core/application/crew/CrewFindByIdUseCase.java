package stb.core.application.crew;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.dto.CrewDetailedDto;
import stb.core.domain.exception.CrewNotFoundException;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.ScheduleSlot;
import stb.core.domain.repository.EngineerCrewRepository;
import stb.core.domain.repository.RequestRepository;
import stb.core.domain.repository.ScheduleSlotRepository;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class CrewFindByIdUseCase {

    private final EngineerCrewRepository engineerCrewRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final RequestRepository requestRepository;

    @Transactional(readOnly = true)
    public CrewDetailedDto execute(int crewId) {
        EngineerCrew crew = engineerCrewRepository
                .findById(crewId)
                .orElseThrow(() -> new CrewNotFoundException("Бригада №%d не найдена".formatted(crewId)));

        LocalDate today = LocalDate.now();

        return CrewDetailedDto.from(
                crew,
                today,
                scheduleSlotRepository.findAllByTargetDateAndCrewIdAndType(today, crewId, ScheduleSlot.SlotType.REQUEST),
                requestRepository.findAllByAssignedCrewId(crewId)
        );
    }
}
