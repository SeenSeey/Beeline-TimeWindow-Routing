package stb.core.api.rest.v1;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import stb.core.api.rest.v1.dto.PageResponseDto;
import stb.core.api.rest.v1.dto.ServerResponse;
import stb.core.api.utils.PaginationUtils;
import stb.core.application.request.*;
import stb.core.domain.dto.*;

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
    private final RequestCreateUseCase requestCreateUseCase;

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

    @PostMapping
    public ResponseEntity<ServerResponse<RequestDto>> createRequest(@RequestBody RequestCreateDto createDto) {
        var request = requestCreateUseCase.execute(createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ServerResponse.ok(HttpStatus.CREATED.value(), request));
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

    /** columns the list can be sorted by: API name → entity property */
    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "id", "id",
            "createdAt", "createdAt",
            "address", "fullAddress",
            // statuses are numbered in lifecycle order: not assigned → … → canceled
            "status", "status.id",
            "jobType", "jobType.displayName",
            "region", "region.displayName",
            "crew", "assignedCrew.name",
            "urgent", "urgent"
    );

    @GetMapping
    public ResponseEntity<ServerResponse<PageResponseDto<RequestDto>>> search(
        @ModelAttribute RequestSearchDto searchDto,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int limit,
        // "<field>,<asc|desc>", the field one of SORTABLE_FIELDS
        @RequestParam(required = false) String sort
    ) {
        Pageable pageable = PaginationUtils.create(page, limit, sort, SORTABLE_FIELDS);

        PageResponseDto<RequestDto> payload = requestSearchUseCase.execute(searchDto, pageable);
        return ResponseEntity.ok(ServerResponse.ok(HttpStatus.OK.value(), payload));
    }
}
