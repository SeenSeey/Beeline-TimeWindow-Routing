package stb.core.application.request;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import stb.core.domain.dto.RequestDto;
import stb.core.domain.dto.RequestSearchDto;
import stb.core.domain.exception.InvalidRequestSearchFiltersException;
import stb.core.domain.repository.RequestRepository;
import stb.core.domain.repository.specification.RequestSpecification;

@Component
public class RequestSearchUseCase {

    private final RequestRepository requestRepository;

    public RequestSearchUseCase(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    public Page<RequestDto> execute(RequestSearchDto searchDto, Pageable pageable) {
        validate(searchDto);

        RequestSpecification specification = new RequestSpecification(searchDto);

        return requestRepository
                .findAll(specification, pageable)
                .map(RequestDto::toDto);
    }

    private void validate(RequestSearchDto searchDto) {
        if (Boolean.FALSE.equals(searchDto.hasAssignedCrew())
                && searchDto.assignedCrewIds() != null
                && !searchDto.assignedCrewIds().isEmpty()) {
            throw new InvalidRequestSearchFiltersException(
                    "Нельзя одновременно искать заявки без бригады "
                            + "и фильтровать их по ID бригад"
            );
        }

        if (searchDto.regionId() != null && searchDto.regionId() <= 0) {
            throw new InvalidRequestSearchFiltersException(
                    "ID региона должен быть положительным числом"
            );
        }

        if (searchDto.statusId() != null && searchDto.statusId() <= 0) {
            throw new InvalidRequestSearchFiltersException(
                    "ID статуса должен быть положительным числом"
            );
        }

        if (searchDto.jobTypeId() != null && searchDto.jobTypeId() <= 0) {
            throw new InvalidRequestSearchFiltersException(
                    "ID типа работ должен быть положительным числом"
            );
        }

        if (searchDto.assignedCrewIds() != null
                && searchDto.assignedCrewIds().stream()
                .anyMatch(id -> id == null || id <= 0)) {
            throw new InvalidRequestSearchFiltersException(
                    "ID бригад должны быть положительными числами"
            );
        }
    }
}
