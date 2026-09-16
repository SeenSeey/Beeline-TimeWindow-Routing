package stb.core.infrastructure.csv.dto;

import java.util.List;

public record DatasetDto(
        List<RequestDto> requests,
        List<EngineerDto> engineers,
        List<ReplanEventDto> events) {
}
