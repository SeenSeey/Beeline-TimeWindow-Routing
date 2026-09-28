package stb.core.infrastructure.route;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisRouteStorage implements RouteStorage {

    private static final String REGION_ROUTES_KEY_PREFIX = "region:routes:";
    // "crew-route:", not the old "route:": values under that prefix hold a flat RouteDto, not legs
    private static final String CREW_ROUTE_KEY_PREFIX = "crew-route:";
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
    public CrewRouteDto findRoute(Schedule schedule, EngineerCrew engineerCrew) {
        String key = buildSingleRouteKey(schedule, engineerCrew);
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
    public void saveRoute(Schedule schedule, EngineerCrew engineerCrew, CrewRouteDto route) {
        redisTemplate.opsForValue().set(buildSingleRouteKey(schedule, engineerCrew), jsonMapper.writeValueAsString(route), ttl);
    }

    @Override
    public void saveRoutes(Region region, List<RouteDto> routes) {
        redisTemplate.opsForValue().set(buildRegionRoutesKey(region), jsonMapper.writeValueAsString(routes), ttl);
    }

    private static String buildRegionRoutesKey(Region region) {
        return REGION_ROUTES_KEY_PREFIX + region.getId();
    }

    private static String buildSingleRouteKey(Schedule schedule, EngineerCrew engineerCrew) {
        return CREW_ROUTE_KEY_PREFIX + schedule.getId() + ":" + engineerCrew.getId();
    }
}
