package stb.core.domain.dto;

import java.util.List;

public record RequestSearchDto(
        Integer statusId,
        Boolean urgent,
        Integer regionId,
        Integer jobTypeId,
        Boolean noAssignedCrew,
        List<Integer> assignedCrewIds,
        // part of the full address, case-insensitive
        String address,
        // part of the assigned crew's name, case-insensitive; requests without a crew never match
        String crewName
) {
}
