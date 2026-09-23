package stb.core.domain.service.scheduler.dto;

import stb.core.domain.service.scheduler.dto.output.OutputSlotDto;

import java.util.List;

// Только модели из stb.core.domain.model
public record ScheduleCalculationResult(
        List<OutputSlotDto> requestDtoList
) {
}
