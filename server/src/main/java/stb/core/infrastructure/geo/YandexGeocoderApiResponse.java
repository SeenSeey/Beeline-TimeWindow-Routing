package stb.core.infrastructure.geo;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import stb.core.domain.dto.GeoPosition;

import java.util.List;

@Slf4j
@Setter(AccessLevel.PROTECTED)
public class YandexGeocoderApiResponse {

    private Response response;

    public GeoPosition toDomainGeoPosition() {
        if (!isValidResponse()) {
            log.error("Некорректный формат ответа: {}", response);
            return null;
        }

        // API возвращает координаты в виде строки формата "+-float[долгота] +-float[широта]"
        String[] entries = extractPositionFromResponse().split(" ");

        if (entries.length != 2) {
            log.error("Некорректный формат координат: {}", extractPositionFromResponse());
            return null;
        }

        float longitude = Float.parseFloat(entries[0]);
        float latitude = Float.parseFloat(entries[1]);

        return new GeoPosition(extractFullAddress(), latitude, longitude);
    }

    private boolean isValidResponse() {
        return response != null
               && response.GeoObjectCollection != null
               && response.GeoObjectCollection.featureMember != null
               && !response.GeoObjectCollection.featureMember.isEmpty()
               && response.GeoObjectCollection.featureMember.getFirst() != null
               && response.GeoObjectCollection.featureMember.getFirst().GeoObject != null
               && response.GeoObjectCollection.featureMember.getFirst().GeoObject.Point != null
               && response.GeoObjectCollection.featureMember.getFirst().GeoObject.Point.pos != null
               && response.GeoObjectCollection.featureMember.getFirst().GeoObject.metaDataProperty != null
               && response.GeoObjectCollection.featureMember.getFirst().GeoObject.metaDataProperty.GeocoderMetaData != null
               && response.GeoObjectCollection.featureMember.getFirst().GeoObject.metaDataProperty.GeocoderMetaData.text != null;
    }

    private String extractPositionFromResponse() {
        // Берём первый для получения совпадения "exact"
        return response.GeoObjectCollection.featureMember.getFirst().GeoObject.Point.pos;
    }

    private String extractFullAddress() {
        return response
                .GeoObjectCollection
                .featureMember
                .getFirst()
                .GeoObject
                .metaDataProperty
                .GeocoderMetaData
                .text;
    }

    private record Response(GeoObjectCollection GeoObjectCollection) {}
    private record GeoObjectCollection(List<FeatureMember> featureMember) {}
    private record FeatureMember(GeoObject GeoObject) {}
    private record GeoObject(MetaDataProperty metaDataProperty, String name, String description, Point Point) {}
    private record MetaDataProperty(GeocoderMetaData GeocoderMetaData) {}
    private record GeocoderMetaData(String text) {}
    private record Point(String pos) {}
}
