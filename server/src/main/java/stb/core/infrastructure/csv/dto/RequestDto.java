package stb.core.infrastructure.csv.dto;

import java.time.LocalTime;

import stb.core.domain.model.JobType;
import stb.core.domain.model.Vehicle;
import stb.core.infrastructure.csv.CsvEquipment;

/**
 * @param requiredVehicle {@code null}, если ограничения по транспорту нет
 * @param requiredEquipment {@code null}, если оборудование не нужно
 */
public record RequestDto(
        int id,
        String address,
        GeoPointDto location,
        int durationMinutes,
        LocalTime windowStart,
        LocalTime windowEnd,
        Vehicle.Keyword requiredVehicle,
        JobType.Keyword jobType,
        CsvEquipment requiredEquipment) {
}
