package stb.core.infrastructure.csv.dto;

import java.time.LocalTime;
import java.util.Set;

import stb.core.domain.model.Skill;
import stb.core.domain.model.Vehicle;

public record EngineerDto(
        int id,
        String name,
        String startAddress,
        GeoPointDto start,
        LocalTime shiftStart,
        LocalTime shiftEnd,
        Set<Skill.Keyword> skills,
        Vehicle.Keyword vehicle) {
}
