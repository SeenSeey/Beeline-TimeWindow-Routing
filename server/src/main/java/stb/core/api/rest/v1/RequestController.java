package stb.core.api.rest.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import stb.core.api.rest.v1.dto.PageResponseDto;
import stb.core.api.rest.v1.dto.ServerResponse;
import stb.core.api.utils.PaginationUtils;
import stb.core.application.request.RequestSearchUseCase;
import stb.core.domain.dto.RequestDto;
import stb.core.domain.dto.RequestSearchDto;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestSearchUseCase requestSearchUseCase;

    @GetMapping
    public ResponseEntity<ServerResponse<PageResponseDto<RequestDto>>> search(
            @ModelAttribute RequestSearchDto searchDto,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {

        Pageable pageable = PaginationUtils.create(page, limit);

        PageResponseDto<RequestDto> payload = requestSearchUseCase.execute(searchDto, pageable);

        return ResponseEntity.ok(ServerResponse.ok(HttpStatus.OK.value(), payload));
    }
}
