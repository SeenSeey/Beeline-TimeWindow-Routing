package stb.core.api.rest.v1;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import stb.core.api.rest.v1.dto.ServerResponse;
import stb.core.application.dev.DevStateResetUseCase;
import stb.core.application.schedule.ScheduleEditUseCase;
import stb.core.application.schedule.ScheduleInitialBuildUseCase;
import stb.core.application.schedule.ScheduleTimelineFindCurrentByRegionUseCase;
import stb.core.domain.dto.ScheduleEditDto;
import stb.core.domain.dto.SchedulePreviewDto;
import stb.core.domain.dto.ScheduleTimelineDto;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleTimelineFindCurrentByRegionUseCase scheduleTimelineFindCurrentByRegionUseCase;
    private final ScheduleInitialBuildUseCase scheduleInitialBuildUseCase;
    private final DevStateResetUseCase devStateResetUseCase;
    private final ScheduleEditUseCase scheduleEditUseCase;

    @GetMapping("/regions/{regionId}")
    public ResponseEntity<ServerResponse<ScheduleTimelineDto>> findCurrentByRegionId(@PathVariable int regionId) {
        var result = scheduleTimelineFindCurrentByRegionUseCase.execute(regionId);
        return ResponseEntity.ok(ServerResponse.ok(HttpStatus.OK.value(), result));
    }

    @PostMapping("/regions/{regionId}/preview")
    public ResponseEntity<ServerResponse<SchedulePreviewDto>> previewEdit(@PathVariable int regionId,
                                                                          @RequestBody ScheduleEditDto edit) {
        var result = scheduleEditUseCase.preview(regionId, edit);
        return ResponseEntity.ok(ServerResponse.ok(HttpStatus.OK.value(), result));
    }

    @PutMapping("/regions/{regionId}/routes")
    public ResponseEntity<ServerResponse<ScheduleTimelineDto>> applyEdit(@PathVariable int regionId,
                                                                         @RequestBody ScheduleEditDto edit) {
        scheduleEditUseCase.apply(regionId, edit);
        var result = scheduleTimelineFindCurrentByRegionUseCase.execute(regionId);
        return ResponseEntity.ok(ServerResponse.ok(HttpStatus.OK.value(), result));
    }

    @PostMapping("/initialize")
    public ResponseEntity<ServerResponse<List<ScheduleTimelineDto>>> createInitialSchedule() {
        var result = scheduleInitialBuildUseCase.execute();
        return ResponseEntity.ok(ServerResponse.ok(HttpStatus.OK.value(), result));
    }

    @PostMapping("/reset")
    public ResponseEntity<ServerResponse<Void>> reset() {
        devStateResetUseCase.execute();

        return ResponseEntity.ok(ServerResponse.ok(HttpStatus.OK.value(), null));
    }
}
