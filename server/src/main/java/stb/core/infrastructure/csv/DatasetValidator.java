package stb.core.infrastructure.csv;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import stb.core.infrastructure.csv.dto.DatasetDto;
import stb.core.infrastructure.csv.dto.EngineerDto;
import stb.core.infrastructure.csv.dto.ReplanEventDto;
import stb.core.infrastructure.csv.dto.RequestDto;

public class DatasetValidator {

    private static final int MAX_SKILLS = 3;

    public void validate(DatasetDto dataset) {
        Set<Integer> requestIds = uniqueIds(dataset.requests(), RequestDto::id, "заявка");
        Set<Integer> engineerIds = uniqueIds(dataset.engineers(), EngineerDto::id, "инженер");
        dataset.requests().forEach(this::validateRequest);
        dataset.engineers().forEach(this::validateEngineer);
        dataset.events().forEach(event -> validateEvent(event, requestIds, engineerIds));
    }

    private void validateRequest(RequestDto request) {
        check(request.durationMinutes() > 0, "заявка %d: длительность должна быть больше 0", request.id());
        check(request.windowStart().isBefore(request.windowEnd()),
                "заявка %d: окно заканчивается не позже, чем начинается", request.id());
    }

    private void validateEngineer(EngineerDto engineer) {
        check(engineer.shiftStart().isBefore(engineer.shiftEnd()),
                "инженер %d: смена заканчивается не позже, чем начинается", engineer.id());
        check(!engineer.skills().isEmpty() && engineer.skills().size() <= MAX_SKILLS,
                "инженер %d: навыков должно быть от 1 до %d", engineer.id(), MAX_SKILLS);
    }

    private void validateEvent(ReplanEventDto event, Set<Integer> requestIds, Set<Integer> engineerIds) {
        switch (event) {
            case ReplanEventDto.CancelRequest cancel -> check(requestIds.contains(cancel.requestId()),
                    "отмена заявки: нет заявки %d", cancel.requestId());
            case ReplanEventDto.EngineerUnavailable unavailable -> check(engineerIds.contains(unavailable.engineerId()),
                    "недоступность инженера: нет инженера %d", unavailable.engineerId());
            case ReplanEventDto.UrgentRequest urgent -> {
                RequestDto request = urgent.request();
                validateRequest(request);
                check(request.urgent(), "срочная заявка %d: должна быть отмечена как срочная", request.id());
                check(!requestIds.contains(request.id()), "срочная заявка %d: id уже занят", request.id());
            }
        }
    }

    private static <T> Set<Integer> uniqueIds(List<T> items, Function<T, Integer> id, String entity) {
        Set<Integer> ids = new HashSet<>();
        for (T item : items) {
            check(ids.add(id.apply(item)), "%s %d: id повторяется", entity, id.apply(item));
        }
        return ids;
    }

    private static void check(boolean condition, String message, Object... args) {
        if (!condition) {
            throw new InvalidDatasetException(message.formatted(args));
        }
    }
}
