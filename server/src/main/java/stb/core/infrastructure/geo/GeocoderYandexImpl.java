package stb.core.infrastructure.geo;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import stb.core.domain.dto.GeoPosition;
import stb.core.domain.port.Geocoder;

import java.net.URI;

/**
 * Реализация интерфейса {@link Geocoder} для API геокодера Яндекс
 */
@Slf4j
@Component
public class GeocoderYandexImpl implements Geocoder {

    private static final String API_KEY_LOGGER_PLACEHOLDER = "XXX";

    private final RestClient restClient;
    private final String apiKey;
    private final String basePath;

    public GeocoderYandexImpl(@Value("${app.geo.geocoder-api-key}") String apiKey,
                              @Value("${app.geo.base-path}") String basePath,
                              RestClient restClient) {

        this.apiKey = apiKey;
        this.basePath = basePath;
        this.restClient = restClient;
    }

    @Override
    public @Nullable GeoPosition convertAddressToCoordinates(String address) {
        try {
            ResponseEntity<YandexGeocoderApiResponse> response = restClient.get()
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

    private URI buildUri(String apiKey, String address) {
        return UriComponentsBuilder.fromUriString(basePath)
                .queryParam("apikey", apiKey)
                .queryParam("geocode", address)
                .queryParam("format", "json")
                .encode()
                .build()
                .toUri();
    }
}
