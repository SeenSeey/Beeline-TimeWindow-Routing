package stb.core.domain.port.route.dto;

import stb.core.domain.model.Request;

public record GeoPoint(double latitude, double longitude) {

    public static GeoPoint from(Request request) {
        return new GeoPoint(request.getLatitude(), request.getLongitude());
    }
}
