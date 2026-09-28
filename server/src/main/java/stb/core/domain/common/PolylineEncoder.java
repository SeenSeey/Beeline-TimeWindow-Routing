package stb.core.domain.common;

import stb.core.domain.port.route.dto.GeoPoint;

import java.util.List;

/**
 * Encoded polyline format (Google's algorithm, precision 5 — about a metre): each coordinate is stored as
 * the difference from the previous one in a few printable characters, latitude first. A point takes
 * 6–8 characters instead of ~45 as a JSON object.
 */
public final class PolylineEncoder {

    private static final double FACTOR = 1e5;

    private PolylineEncoder() {
    }

    public static String encode(List<GeoPoint> points) {
        StringBuilder encoded = new StringBuilder();
        long previousLatitude = 0;
        long previousLongitude = 0;

        for (GeoPoint point : points) {
            long latitude = Math.round(point.latitude() * FACTOR);
            long longitude = Math.round(point.longitude() * FACTOR);

            encodeValue(latitude - previousLatitude, encoded);
            encodeValue(longitude - previousLongitude, encoded);

            previousLatitude = latitude;
            previousLongitude = longitude;
        }

        return encoded.toString();
    }

    private static void encodeValue(long value, StringBuilder encoded) {
        // the sign goes into the lowest bit, then 5-bit chunks from the lowest, each but the last flagged with 0x20
        long shifted = value < 0 ? ~(value << 1) : value << 1;

        while (shifted >= 0x20) {
            encoded.append((char) ((0x20 | (shifted & 0x1f)) + 63));
            shifted >>= 5;
        }

        encoded.append((char) (shifted + 63));
    }
}
