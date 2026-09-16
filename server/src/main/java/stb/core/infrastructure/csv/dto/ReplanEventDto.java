package stb.core.infrastructure.csv.dto;

import java.time.LocalTime;

public sealed interface ReplanEventDto {

    LocalTime eventTime();

    record UrgentRequest(LocalTime eventTime, RequestDto request) implements ReplanEventDto {
    }

    record CancelRequest(LocalTime eventTime, int requestId) implements ReplanEventDto {
    }

    record EngineerUnavailable(LocalTime eventTime, int engineerId) implements ReplanEventDto {
    }
}
