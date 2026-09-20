package stb.core.api.rest.v1;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import stb.core.api.rest.v1.dto.PageResponseDto;
import stb.core.api.rest.v1.dto.ServerResponse;
import stb.core.application.request.RequestSearchUseCase;
import stb.core.domain.dto.RequestDto;
import stb.core.domain.dto.RequestSearchDto;
import stb.core.domain.exception.InvalidRequestSearchFiltersException;

@RestController
@RequestMapping("/api/v1/requests")
public class RequestController {

    private final RequestSearchUseCase useCase;

    public RequestController(RequestSearchUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public ResponseEntity<ServerResponse<PageResponseDto<RequestDto>>> search(
            @ModelAttribute RequestSearchDto searchDto,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        validatePagination(page, limit);

        Pageable pageable = PageRequest.of(page - 1, limit);

        Page<RequestDto> result = useCase.execute(searchDto, pageable);

        PageResponseDto<RequestDto> payload = PageResponseDto.toDto(result);

        ServerResponse<PageResponseDto<RequestDto>> response =
                ServerResponse.ok(
                        HttpStatus.OK.value(),
                        payload
                );

        return ResponseEntity.ok(response);
    }

    // Не знаю насколько хорошо иметь валидацию в контроллере
    private void validatePagination(int page, int limit) {
        if (page < 1) {
            throw new InvalidRequestSearchFiltersException(
                    "Номер страницы должен быть не меньше 1"
            );
        }

        if (limit < 1) {
            throw new InvalidRequestSearchFiltersException(
                    "Размер страницы должен находиться в диапазоне от 1 до "
            );
        }
    }
}
