package stb.core.domain.dto;

import lombok.Builder;
import stb.core.domain.model.EngineerCrew;

import java.util.List;

@Builder
public record CrewDto(
        int id,
        String name,
        String regionName,
        String vehicle,
        List<String> skills
) {

    public static CrewDto toDto(EngineerCrew model) {
        return CrewDto.builder()
                .id(model.getId())
                .name(model.getName())
                .regionName(model.getRegion().getDisplayName())
                .vehicle(model.getVehicle().getKeyword().name())
                .skills(model.getSkills().stream()
                        .map(skill -> skill.getKeyword().name())
                        .toList())
                .build();
    }

}
