package stb.core.infrastructure.route;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Region;
import stb.core.domain.model.Schedule;
import stb.core.domain.port.route.RouteStorage;
import stb.core.domain.port.route.dto.CrewRouteDto;
import stb.core.domain.port.route.dto.RouteDto;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisRouteStorage implements RouteStorage {

    private static final String REGION_ROUTES_KEY_PREFIX = "region:routes:";
    private static final String CREW_ROUTE_KEY_PREFIX = "crew-route:";
    private static final int CLEAR_BATCH_SIZE = 500;
    private static final TypeReference<List<RouteDto>> ROUTE_LIST_TYPE = new TypeReference<>() {};

    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;

    @Value("${app.route.storage-ttl:48h}")
    private Duration ttl;

    @Override
    public List<RouteDto> findAllRoutesByRegion(Region region) {
        String key = buildRegionRoutesKey(region);
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            return List.of();
        }

        try {
            return jsonMapper.readValue(json, ROUTE_LIST_TYPE);
        } catch (JacksonException exception) {
            log.error("Не удалось прочитать маршруты по ключу {}", key, exception);
            return List.of();
        }
    }

    @Override
    @Nullable
    public CrewRouteDto findRoute(Region region, EngineerCrew engineerCrew) {
        String key = buildSingleRouteKey(region, engineerCrew);
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            return null;
        }

        try {
            return jsonMapper.readValue(json, CrewRouteDto.class);
        } catch (JacksonException exception) {
            log.error("Не удалось прочитать маршрут по ключу {}", key, exception);
            return null;
        }
    }

    @Override
    public void saveRoute(Region region, EngineerCrew engineerCrew, CrewRouteDto route) {
        redisTemplate.opsForValue().set(buildSingleRouteKey(region, engineerCrew), jsonMapper.writeValueAsString(route), ttl);
    }

    @Override
    public void saveRoutes(Region region, List<RouteDto> routes) {
        redisTemplate.opsForValue().set(buildRegionRoutesKey(region), jsonMapper.writeValueAsString(routes), ttl);
    }

    @Override
    public void clearAll() {
        long deleted = 0;

        for (String prefix : List.of(REGION_ROUTES_KEY_PREFIX, CREW_ROUTE_KEY_PREFIX)) {
            deleted += deleteByPrefix(prefix);
        }

        log.info("Хранилище маршрутов очищено, удалено ключей: {}", deleted);
    }

    @Override
    public void clearCrewsRoutesByRegion(Region region) {
        // this region's crews only: "crew-route:<regionId>:" — the bare prefix would take every region's
        long deleted = deleteByPrefix(CREW_ROUTE_KEY_PREFIX + region.getId() + ":");
        log.info("Маршруты бригад региона {} удалены из хранилища, удалено ключей: {}", region.getId(), deleted);
    }

    private long deleteByPrefix(String prefix) {
        ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(CLEAR_BATCH_SIZE).build();
        long deleted = 0;
        List<String> batch = new ArrayList<>(CLEAR_BATCH_SIZE);

        try (Cursor<String> keys = redisTemplate.scan(options)) {
            while (keys.hasNext()) {
                batch.add(keys.next());
                if (batch.size() == CLEAR_BATCH_SIZE) {
                    deleted += deleteBatch(batch);
                }
            }
        }
        return deleted + deleteBatch(batch);
    }

    private long deleteBatch(List<String> batch) {
        if (batch.isEmpty()) {
            return 0;
        }
        Long deleted = redisTemplate.delete(batch);
        batch.clear();
        return deleted == null ? 0 : deleted;
    }

    private static String buildRegionRoutesKey(Region region) {
        return REGION_ROUTES_KEY_PREFIX + region.getId();
    }

    private static String buildSingleRouteKey(Region region, EngineerCrew engineerCrew) {
        return CREW_ROUTE_KEY_PREFIX + region.getId() + ":" + engineerCrew.getId();
    }
}
