package stb.core.domain.port.route.dto;

import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Region;
import stb.core.domain.model.Request;

public record Location(String id, double latitude, double longitude) {

    public static Location from(String id, GeoPoint point) {
        return new Location(id, point.latitude(), point.longitude());
    }

    public static Location from(Request request) {
        return new Location("R_" + request.getId(), request.getLatitude(), request.getLongitude());
    }

    public static Location from(Region region) {
        return new Location("O_" + region.getId(), region.getOfficeLatitude(), region.getOfficeLongitude());
    }

    public static Location from(EngineerCrew crew) {
        return from(crew.getRegion());
    }
}
