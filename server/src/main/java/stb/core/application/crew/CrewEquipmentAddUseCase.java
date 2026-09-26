package stb.core.application.crew;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.dto.CrewEquipmentAddDto;
import stb.core.domain.exception.CrewNotFoundException;
import stb.core.domain.exception.DomainException;
import stb.core.domain.exception.EquipmentNotFoundException;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Equipment;
import stb.core.domain.repository.EngineerCrewRepository;
import stb.core.domain.repository.EquipmentRepository;

@Component
@RequiredArgsConstructor
public class CrewEquipmentAddUseCase {

    private final EngineerCrewRepository engineerCrewRepository;
    private final EquipmentRepository equipmentRepository;

    @Transactional
    public void execute(int crewId, CrewEquipmentAddDto dto) {
        if (dto == null || dto.quantity() <= 0) {
            throw new DomainException.ValidationException("Количество оборудования должно быть больше 0");
        }

        EngineerCrew crew = engineerCrewRepository
                .findById(crewId)
                .orElseThrow(() -> new CrewNotFoundException("Бригада №%d не найдена".formatted(crewId)));
        Equipment equipment = equipmentRepository
                .findById(dto.equipmentId())
                .orElseThrow(() -> new EquipmentNotFoundException("Оборудование №%d не найдено".formatted(dto.equipmentId())));

        crew.addEquipment(equipment, dto.quantity());
        engineerCrewRepository.save(crew);
    }
}
