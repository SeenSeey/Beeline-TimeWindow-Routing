package stb.core.application.dictionary;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.dto.DictionaryItemDto;
import stb.core.domain.repository.EquipmentRepository;
import stb.core.domain.repository.JobTypeRepository;
import stb.core.domain.repository.RequestStatusRepository;
import stb.core.domain.repository.SkillRepository;
import stb.core.domain.repository.VehicleRepository;

import java.util.List;

/** Словари для выпадающих списков и фильтров клиента: всё с сервера, с настоящими ID */
@Component
@RequiredArgsConstructor
public class DictionaryFindAllUseCase {

    /** in the order they were created: statuses in their lifecycle order, enums in declaration order */
    private static final Sort BY_ID = Sort.by("id");

    private final RequestStatusRepository requestStatusRepository;
    private final JobTypeRepository jobTypeRepository;
    private final VehicleRepository vehicleRepository;
    private final SkillRepository skillRepository;
    private final EquipmentRepository equipmentRepository;

    @Transactional(readOnly = true)
    public List<DictionaryItemDto> requestStatuses() {
        return requestStatusRepository.findAll(BY_ID).stream().map(DictionaryItemDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DictionaryItemDto> jobTypes() {
        return jobTypeRepository.findAll(BY_ID).stream().map(DictionaryItemDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DictionaryItemDto> vehicles() {
        return vehicleRepository.findAll(BY_ID).stream().map(DictionaryItemDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DictionaryItemDto> skills() {
        return skillRepository.findAll(BY_ID).stream().map(DictionaryItemDto::from).toList();
    }

    /** by name: there is no natural order to equipment */
    @Transactional(readOnly = true)
    public List<DictionaryItemDto> equipment() {
        return equipmentRepository.findAll(Sort.by("displayName")).stream().map(DictionaryItemDto::from).toList();
    }
}
