package stb.core.domain.port.route.dto;

import stb.core.domain.common.GeopositionProvider;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Region;
import stb.core.domain.model.Request;

public record Location(String id, double latitude, double longitude) {

    public static Location from(GeopositionProvider geopositionProvider) {
        return new Location(buildId(geopositionProvider), geopositionProvider.getLatitude(), geopositionProvider.getLongitude());
    }

    public static String buildId(GeopositionProvider geopositionProvider) {
        return geopositionProvider.getLatitude() + "_" + geopositionProvider.getLongitude();
    }
}
