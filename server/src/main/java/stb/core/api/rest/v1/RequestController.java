package stb.core.api.rest.v1;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import stb.core.api.rest.v1.dto.PageResponseDto;
import stb.core.api.rest.v1.dto.ServerResponse;
import stb.core.api.utils.PaginationUtils;
import stb.core.application.request.*;
import stb.core.domain.dto.RequestDetailedDto;
import stb.core.domain.dto.RequestDto;
import stb.core.domain.dto.RequestMapPointDto;
import stb.core.domain.dto.RequestSearchDto;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestSearchUseCase requestSearchUseCase;
    private final RequestCancelUseCase requestCancelUseCase;
    private final RequestFindByIdUseCase requestFindByIdUseCase;
    private final RequestCompleteUseCase requestCompleteUseCase;
    private final RequestMarkInProgressUseCase requestMarkInProgressUseCase;
    private final RequestMapPointsFindByRegionUseCase requestMapPointsFindByRegionUseCase;

    @GetMapping("/map")
    public ResponseEntity<ServerResponse<List<RequestMapPointDto>>> findMapPoints(@RequestParam int regionId) {
        return ResponseEntity.ok(
            ServerResponse.ok(HttpStatus.OK.value(), requestMapPointsFindByRegionUseCase.execute(regionId))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServerResponse<RequestDetailedDto>> findById(@PathVariable int id) {
        return ResponseEntity.ok(ServerResponse.ok(HttpStatus.OK.value(), requestFindByIdUseCase.execute(id)));
    }

    @PatchMapping("/{id}/in_progress")
    public ResponseEntity<?> markInProgressRequestById(@PathVariable int id) {
        requestMarkInProgressUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<?> completeRequestById(@PathVariable int id) {
        requestCompleteUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelRequestById(@PathVariable int id) {
        requestCancelUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

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
