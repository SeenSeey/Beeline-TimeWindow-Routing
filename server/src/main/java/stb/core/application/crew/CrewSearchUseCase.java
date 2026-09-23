package stb.core.application.crew;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import stb.core.api.rest.v1.dto.PageResponseDto;
import stb.core.domain.dto.CrewDto;
import stb.core.domain.dto.CrewSearchDto;
import stb.core.domain.repository.EngineerCrewRepository;
import stb.core.domain.repository.specification.EngineerCrewSpecification;

@Component
@RequiredArgsConstructor
public class CrewSearchUseCase {

    private final EngineerCrewRepository engineerCrewRepository;

    public PageResponseDto<CrewDto> execute(CrewSearchDto searchDto, Pageable pageable) {

        EngineerCrewSpecification specification = new EngineerCrewSpecification(searchDto);

        return PageResponseDto.toDto(engineerCrewRepository
                .findAll(specification, pageable)
                .map(CrewDto::toDto));
    }
}
