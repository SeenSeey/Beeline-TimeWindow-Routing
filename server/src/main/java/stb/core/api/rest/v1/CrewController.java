package stb.core.api.rest.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import stb.core.api.rest.v1.dto.PageResponseDto;
import stb.core.api.rest.v1.dto.ServerResponse;
import stb.core.api.utils.PaginationUtils;
import stb.core.application.crew.CrewSearchUseCase;
import stb.core.domain.dto.CrewDto;
import stb.core.domain.dto.CrewSearchDto;

@RestController
@RequestMapping("/api/v1/crews")
@RequiredArgsConstructor
public class CrewController {

    private final CrewSearchUseCase crewSearchUseCase;

    @GetMapping
    public ResponseEntity<ServerResponse<PageResponseDto<CrewDto>>> search(
            @ModelAttribute CrewSearchDto searchDto,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {

        Pageable pageable = PaginationUtils.create(page, limit);

        PageResponseDto<CrewDto> payload = crewSearchUseCase.execute(searchDto, pageable);

        return ResponseEntity.ok(ServerResponse.ok(HttpStatus.OK.value(), payload));
    }
}
