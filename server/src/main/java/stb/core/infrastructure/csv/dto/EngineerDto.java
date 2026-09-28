package stb.core.infrastructure.csv.dto;

import java.time.LocalTime;
import java.util.Set;

import stb.core.domain.model.Vehicle;
import stb.core.infrastructure.csv.CsvSkill;

public record EngineerDto(
        int id,
        String name,
        String startAddress,
        GeoPointDto start,
        LocalTime shiftStart,
        LocalTime shiftEnd,
        Set<CsvSkill> skills,
        Vehicle.Keyword vehicle,
        String district) {
}
