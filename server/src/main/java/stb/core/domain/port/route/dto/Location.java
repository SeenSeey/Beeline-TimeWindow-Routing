package stb.core.domain.port.route.dto;

import stb.core.domain.common.GeopositionProvider;

public record Location(String id, double latitude, double longitude) {

    public static Location from(GeopositionProvider geopositionProvider) {
        return new Location(buildId(geopositionProvider), geopositionProvider.getLatitude(), geopositionProvider.getLongitude());
    }

    public static String buildId(GeopositionProvider geopositionProvider) {
        return geopositionProvider.getLatitude() + "_" + geopositionProvider.getLongitude();
    }
}
