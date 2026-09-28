package stb.core.domain.dto;

import lombok.Builder;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Request;

import java.time.LocalDateTime;

@Builder
public record RequestDto(
        int id,
        String fullAddress,
        String status,
        boolean urgent,
        RegionDto region,
        DictionaryDto jobType,
        AssignedCrewDto assignedCrew,
        LocalDateTime createdAt
) {

    public record AssignedCrewDto(int id, String name) {

        public static AssignedCrewDto from(EngineerCrew crew) {
            return crew == null ? null : new AssignedCrewDto(crew.getId(), crew.getName());
        }
    }

    public static RequestDto toDto(Request model) {
        return RequestDto.builder()
                .id(model.getId())
                .fullAddress(model.getFullAddress())
                .status(model.getStatus().getKeyword().name())
                .urgent(model.isUrgent())
                .region(RegionDto.from(model.getRegion()))
                .jobType(DictionaryDto.from(model.getJobType()))
                .assignedCrew(AssignedCrewDto.from(model.getAssignedCrew()))
                .createdAt(model.getCreatedAt())
                .build();
    }

}
