package stb.core.infrastructure.csv.dto;

import java.time.LocalTime;

import stb.core.domain.model.Skill;
import stb.core.domain.model.Vehicle;

/**
 * @param requiredVehicle {@code null}, если ограничения по транспорту нет
 */
public record RequestDto(
        int id,
        String address,
        GeoPointDto location,
        int durationMinutes,
        LocalTime windowStart,
        LocalTime windowEnd,
        boolean urgent,
        Skill.Keyword requiredSkill,
        Vehicle.Keyword requiredVehicle) {
}
