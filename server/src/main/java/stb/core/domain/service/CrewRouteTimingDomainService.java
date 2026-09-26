package stb.core.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Equipment;
import stb.core.domain.model.Request;
import stb.core.domain.model.Vehicle;
import stb.core.domain.port.route.RouteBuilder;
import stb.core.domain.port.route.dto.Location;
import stb.core.domain.service.scheduler.impl.logic.speed.SpeedProfile;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class CrewRouteTimingDomainService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final RouteBuilder routeBuilder;
    private final SpeedProfileDomainService speedProfileDomainService;

    public record Stop(Request request, LocalTime pinnedStart) {
    }

    public record PlannedVisit(Request request,
                               LocalTime arrivalTime,
                               LocalTime startTime,
                               LocalTime endTime,
                               List<String> violations) {

        public boolean feasible() {
            return violations.isEmpty();
        }
    }

    public List<PlannedVisit> planRoute(EngineerCrew crew, List<Stop> stopsInOrder) {
        SpeedProfile speedProfile = speedProfileDomainService.createSpeedProfile();
        double speedKmh = crew.getVehicle().getAverageSpeedKmh();

        List<Request> requests = new ArrayList<>();
        List<LocalTime> arrivals = new ArrayList<>();
        List<LocalTime> starts = new ArrayList<>();
        List<LocalTime> ends = new ArrayList<>();
        List<List<String>> violations = new ArrayList<>();

        Location previousLocation = Location.from(crew);
        LocalTime previousDeparture = crew.getShiftStartTime();

        for (Stop stop : stopsInOrder) {
            Request request = stop.request();
            LocalTime arrival = previousDeparture == null
                    ? null
                    : speedProfile.calculateArrivalTime(distanceKm(previousLocation, Location.from(request)), speedKmh, previousDeparture);
            LocalTime start = stop.pinnedStart() != null
                    ? stop.pinnedStart()
                    : arrival == null ? null : latest(arrival, request.getStartTimeWindow());
            LocalTime end = endOfWork(start, request);

            requests.add(request);
            arrivals.add(arrival);
            starts.add(start);
            ends.add(end);
            violations.add(ownViolationsOf(crew, request, start, end));

            previousLocation = Location.from(request);
            previousDeparture = end;
        }

        addOverlaps(requests, starts, ends, violations);
        addMissingEquipment(crew, requests, violations);

        return IntStream.range(0, requests.size())
                .mapToObj(i -> new PlannedVisit(requests.get(i), arrivals.get(i), starts.get(i), ends.get(i), violations.get(i)))
                .toList();
    }

    private List<String> ownViolationsOf(EngineerCrew crew, Request request, LocalTime start, LocalTime end) {
        List<String> violations = new ArrayList<>();

        Vehicle requiredVehicle = request.getRequiredVehicle();
        if (requiredVehicle != null && requiredVehicle.getKeyword() != crew.getVehicle().getKeyword()) {
            violations.add("нужен транспорт «%s», у бригады — «%s»".formatted(
                    requiredVehicle.getDisplayName(), crew.getVehicle().getDisplayName()));
        }

        if (start == null || end == null) {
            violations.add("время работы не укладывается в сутки");
            return violations;
        }

        LocalTime windowStart = request.getStartTimeWindow();
        LocalTime windowEnd = request.getEndTimeWindow();
        if (start.isBefore(windowStart)) {
            violations.add("работа начнётся в %s — раньше окна %s–%s".formatted(TIME.format(start), TIME.format(windowStart), TIME.format(windowEnd)));
        } else if (start.isAfter(windowEnd)) {
            violations.add("работа начнётся в %s — позже окна %s–%s".formatted(TIME.format(start), TIME.format(windowStart), TIME.format(windowEnd)));
        }
        return violations;
    }

    private static void addOverlaps(List<Request> requests, List<LocalTime> starts, List<LocalTime> ends,
                                    List<List<String>> violations) {
        List<Integer> timed = IntStream.range(0, requests.size())
                .filter(i -> starts.get(i) != null && ends.get(i) != null)
                .boxed()
                .sorted(Comparator.comparing(starts::get))
                .toList();

        for (int a = 0; a < timed.size(); a++) {
            int first = timed.get(a);
            for (int b = a + 1; b < timed.size(); b++) {
                int second = timed.get(b);
                if (!starts.get(second).isBefore(ends.get(first))) {
                    break;
                }
                violations.get(first).add(overlapMessage(requests.get(second), starts.get(second), ends.get(second)));
                violations.get(second).add(overlapMessage(requests.get(first), starts.get(first), ends.get(first)));
            }
        }
    }

    private static void addMissingEquipment(EngineerCrew crew, List<Request> requests, List<List<String>> violations) {
        Map<Integer, Integer> left = new HashMap<>();
        crew.getEquipment().forEach(item -> left.put(item.getEquipment().getId(), item.getQuantity()));

        for (int i = 0; i < requests.size(); i++) {
            Request request = requests.get(i);
            Equipment required = request.getRequiredEquipment();
            if (required == null || request.isInProgress() || request.isCompleted()) {
                continue;
            }
            int onHand = left.merge(required.getId(), -1, Integer::sum);
            if (onHand < 0) {
                violations.get(i).add("у бригады не хватает оборудования «%s»".formatted(required.getDisplayName()));
            }
        }
    }

    private static String overlapMessage(Request other, LocalTime otherStart, LocalTime otherEnd) {
        return "пересекается с заявкой №%d (%s–%s)".formatted(other.getId(), TIME.format(otherStart), TIME.format(otherEnd));
    }

    private static LocalTime endOfWork(LocalTime start, Request request) {
        if (start == null) {
            return null;
        }
        LocalTime end = start.plusMinutes(request.getJobType().getTotalTimeSpanMinutes());
        return end.isBefore(start) ? null : end;
    }

    private static LocalTime latest(LocalTime first, LocalTime second) {
        return first.isAfter(second) ? first : second;
    }

    private double distanceKm(Location from, Location to) {
        return routeBuilder.buildRoute(from, to, Vehicle.Keyword.CAR).distanceKilometers();
    }
}
