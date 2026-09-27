package stb.core.domain.dto;

import java.time.LocalTime;

public record RequestCreateDto(int regionId,
                               String address,
                               int jobTypeId,
                               Integer requiredVehicleId,
                               Integer requiredEquipmentId,
                               LocalTime startTimeWindow,
                               LocalTime endTimeWindow) {
}
