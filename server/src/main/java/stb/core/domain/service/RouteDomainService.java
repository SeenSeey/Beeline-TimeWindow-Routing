package stb.core.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stb.core.domain.dto.RequestRouteCalculationDto;
import stb.core.domain.model.Request;
import stb.core.domain.model.Vehicle;
import stb.core.domain.port.route.RouteBuilder;
import stb.core.domain.port.route.dto.RouteDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteDomainService {

    private final RouteBuilder routeBuilder;

    /**
     * Рассчитать расстояние между всеми заявками. Метод работает с {@link RequestRouteCalculationDto} вместо
     * модели {@link Request} во избежании блокировок со стороны транзакций Spring. Процесс расчёта расстояний
     * занимает долгое время, из-за чего могут возникать блокировки при запросе данных во время работы алгоритма.
     *
     * @param requests список заявок
     * @param vehicleType тип транспорта
     * @return список {@link RouteDto}
     */
    public List<RouteDto> calculateAllRoutes(List<RequestRouteCalculationDto> requests, Vehicle.Keyword vehicleType) {
        long timeStart = System.currentTimeMillis();

        int poolSize = Runtime.getRuntime().availableProcessors() + 1;
        int amount = (requests.size() * requests.size() - requests.size()) / 2;

        log.info("Начало расчёта всех маршрутов между {} заявками. Размер пула: {}", requests.size(), poolSize);

        try (ExecutorService threadPool = Executors.newFixedThreadPool(poolSize)) {
            List<Future<RouteDto>> futures = new ArrayList<>();
            List<RouteDto> routes = new ArrayList<>(amount);

            for (int i = 0; i < requests.size() - 1; i++) {
                for (int j = i + 1; j < requests.size(); j++) {
                    final int i_ = i;
                    final int j_ = j;

                    futures.add(CompletableFuture.supplyAsync(() ->
                            routeBuilder.buildRoute(
                                    requests.get(i_).point(),
                                    requests.get(j_).point(),
                                    vehicleType
                            ), threadPool));
                }
            }

            for (Future<RouteDto> future : futures) {
                routes.add(future.get(1000, TimeUnit.MILLISECONDS));
            }

            log.info("Расчёт всех маршрутов завершён за {} мс. Ожидаемое количество: {}, фактическое количество: {}",
                    System.currentTimeMillis() - timeStart,
                    amount,
                    routes.size());

            return routes;
        } catch (Exception exception) {
            log.error("Ошибка во время расчёта всех маршрутов", exception);
            return Collections.emptyList();
        }
    }
}
