package stb.core.domain.dto;

public record CrewSearchDto(
        String name,
        Integer regionId,
        Integer vehicleId,
        Integer skillId
) {
}
