package stb.core.domain.dto;

import stb.core.domain.model.Region;

public record RegionDto(int id, String displayName, String officeAddress) {

    public static RegionDto from(Region region) {
        return new RegionDto(
                region.getId(),
                region.getDisplayName(),
                region.getOfficeAddress()
        );
    }
}
