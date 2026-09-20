package stb.core.infrastructure.geo;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import stb.core.domain.dto.GeoPosition;
import stb.core.domain.port.Geocoder;

/**
 * Реализация интерфейса {@link Geocoder} для API геокодера Яндекс
 */
@Slf4j
@Component
public class GeocoderYandexImpl implements Geocoder {

    private static final String API_KEY_LOGGER_PLACEHOLDER = "XXX";

    @Value("${app.geo.geocoder-api-key}")
    private String apiKey;

    @Value("${app.geo.base-path}")
    private String basePath;

    @Override
    public @Nullable GeoPosition convertAddressToCoordinates(String address) {
        try {
            RestClient client = RestClient.create();

            ResponseEntity<YandexGeocoderApiResponse> response = client.get()
                    .uri(buildUri(apiKey, address))
                    .retrieve()
                    .toEntity(YandexGeocoderApiResponse.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Ошибка получения данных адреса геокодера {}: {}", response.getStatusCode(), response.getBody());
                return null;
            }

            // Не используем !response.hasBody() для корректной работы статического анализатора
            if (response.getBody() == null) {
                log.error("API геокодера не предоставил тело ответа для запроса: {}", buildUri(API_KEY_LOGGER_PLACEHOLDER, address));
                return null;
            }

            GeoPosition geoPosition = response.getBody().toDomainGeoPosition();

            if (geoPosition == null) {
                return null;
            }

            log.info("Результат геокодинга для запроса: {}. Ответ [lat, lon]: [{}, {}]", address, geoPosition.latitude(), geoPosition.longitude());
            return geoPosition;
        } catch (Exception exception) {
            log.error("Ошибка при выполнении запроса геокодера", exception);
            return null;
        }
    }

    private String buildUri(String apiKey, String address) {
        return "%s?apikey=%s&geocode=%s&format=json".formatted(basePath, apiKey, address);
    }
}
