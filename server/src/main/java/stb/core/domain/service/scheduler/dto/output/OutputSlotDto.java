package stb.core.domain.service.scheduler.dto.output;

import java.time.LocalTime;

public record OutputSlotDto(
        int requestId,
        Integer engineerId,
        int serviceDurationMinutes,
        LocalTime windowStart,
        LocalTime windowEnd,
        LocalTime arrivalTime,
        LocalTime serviceStartTime,
        LocalTime departureTime,
        Integer previousRequestId
) {
}
