package stb.core.application.request;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import stb.core.api.rest.v1.dto.PageResponseDto;
import stb.core.domain.dto.RequestDto;
import stb.core.domain.dto.RequestSearchDto;
import stb.core.domain.repository.RequestRepository;
import stb.core.domain.repository.specification.RequestSpecification;

@Component
public class RequestSearchUseCase {

    private final RequestRepository requestRepository;

    public RequestSearchUseCase(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    public PageResponseDto<RequestDto> execute(RequestSearchDto searchDto, Pageable pageable) {

        RequestSpecification specification = new RequestSpecification(searchDto);

        return PageResponseDto.toDto(requestRepository
                .findAll(specification, pageable)
                .map(RequestDto::toDto));
    }
}
