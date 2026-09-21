package stb.core.domain.dto;

import stb.core.domain.port.route.dto.GeoPoint;

public record RequestRouteCalculationDto(int requestId, GeoPoint point) {
}
