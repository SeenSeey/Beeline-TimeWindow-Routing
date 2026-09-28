package stb.core.api.rest.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import stb.core.api.rest.v1.dto.PageResponseDto;
import stb.core.api.rest.v1.dto.ServerResponse;
import stb.core.api.utils.PaginationUtils;
import stb.core.application.crew.CrewEquipmentAddUseCase;
import stb.core.application.crew.CrewFindByIdUseCase;
import stb.core.application.crew.CrewOffShiftUseCase;
import stb.core.application.crew.CrewSearchUseCase;
import stb.core.domain.dto.CrewDetailedDto;
import stb.core.domain.dto.CrewDto;
import stb.core.domain.dto.CrewEquipmentAddDto;
import stb.core.domain.dto.CrewSearchDto;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/crews")
@RequiredArgsConstructor
public class CrewController {

    private final CrewSearchUseCase crewSearchUseCase;
    private final CrewFindByIdUseCase crewFindByIdUseCase;
    private final CrewEquipmentAddUseCase crewEquipmentAddUseCase;
    private final CrewOffShiftUseCase crewOffShiftUseCase;

    /** columns the list can be sorted by: API name → entity property */
    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "id", "id",
            "name", "name",
            "region", "region.displayName",
            "vehicle", "vehicle.displayName"
    );

    @GetMapping
    public ResponseEntity<ServerResponse<PageResponseDto<CrewDto>>> search(
            @ModelAttribute CrewSearchDto searchDto,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            // "<field>,<asc|desc>", the field one of SORTABLE_FIELDS
            @RequestParam(required = false) String sort
    ) {

        Pageable pageable = PaginationUtils.create(page, limit, sort, SORTABLE_FIELDS);

        PageResponseDto<CrewDto> payload = crewSearchUseCase.execute(searchDto, pageable);

        return ResponseEntity.ok(ServerResponse.ok(HttpStatus.OK.value(), payload));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServerResponse<CrewDetailedDto>> findById(@PathVariable int id) {
        return ResponseEntity.ok(ServerResponse.ok(HttpStatus.OK.value(), crewFindByIdUseCase.execute(id)));
    }

    @PostMapping("/{id}/equipment")
    public ResponseEntity<ServerResponse<CrewDetailedDto>> addEquipment(@PathVariable int id,
                                                                        @RequestBody CrewEquipmentAddDto dto) {
        crewEquipmentAddUseCase.execute(id, dto);
        return ResponseEntity.ok(ServerResponse.ok(HttpStatus.OK.value(), crewFindByIdUseCase.execute(id)));
    }

    @PatchMapping("/{crewId}/offShift")
    public ResponseEntity<?> takeCrewOffShift(@PathVariable int crewId) {
        crewOffShiftUseCase.execute(crewId);
        return ResponseEntity.noContent().build();
    }
}
