package stb.core.api.rest.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import stb.core.api.rest.v1.dto.ServerResponse;
import stb.core.application.dictionary.DictionaryFindAllUseCase;
import stb.core.domain.dto.DictionaryItemDto;

import java.util.List;

/** The dictionaries the client's selectors and filters offer, with their real ids */
@RestController
@RequestMapping("/api/v1/dictionaries")
@RequiredArgsConstructor
public class DictionaryController {

    private final DictionaryFindAllUseCase dictionaryFindAllUseCase;

    @GetMapping("/request-statuses")
    public ResponseEntity<ServerResponse<List<DictionaryItemDto>>> requestStatuses() {
        return ok(dictionaryFindAllUseCase.requestStatuses());
    }

    @GetMapping("/job-types")
    public ResponseEntity<ServerResponse<List<DictionaryItemDto>>> jobTypes() {
        return ok(dictionaryFindAllUseCase.jobTypes());
    }

    @GetMapping("/vehicles")
    public ResponseEntity<ServerResponse<List<DictionaryItemDto>>> vehicles() {
        return ok(dictionaryFindAllUseCase.vehicles());
    }

    @GetMapping("/skills")
    public ResponseEntity<ServerResponse<List<DictionaryItemDto>>> skills() {
        return ok(dictionaryFindAllUseCase.skills());
    }

    @GetMapping("/equipment")
    public ResponseEntity<ServerResponse<List<DictionaryItemDto>>> equipment() {
        return ok(dictionaryFindAllUseCase.equipment());
    }

    private static ResponseEntity<ServerResponse<List<DictionaryItemDto>>> ok(List<DictionaryItemDto> items) {
        return ResponseEntity.ok(ServerResponse.ok(HttpStatus.OK.value(), items));
    }
}
