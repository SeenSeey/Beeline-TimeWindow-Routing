package stb.core.domain.port;

import jakarta.annotation.Nullable;
import stb.core.domain.dto.GeoPosition;

public interface Geocoder {

    @Nullable GeoPosition convertAddressToCoordinates(String address);
}
