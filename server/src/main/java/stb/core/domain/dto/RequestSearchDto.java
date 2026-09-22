package stb.core.domain.dto;

import java.util.List;

public record RequestSearchDto(
        Integer statusId,
        Boolean urgent,
        Integer regionId,
        Integer jobTypeId,
        Boolean noAssignedCrew,
        List<Integer> assignedCrewIds
) {
}
