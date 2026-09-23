package stb.core.api.rest.v1;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import stb.core.api.rest.v1.dto.ServerResponse;
import stb.core.application.region.RegionFindAllUseCase;
import stb.core.domain.dto.RegionDto;

@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionFindAllUseCase regionFindAllUseCase;

    @GetMapping
    public ResponseEntity<ServerResponse<List<RegionDto>>> findAll() {
        var result = regionFindAllUseCase.execute();
        return ResponseEntity.ok(ServerResponse.ok(HttpStatus.OK.value(), result));
    }
}
