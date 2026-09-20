package stb.core.domain.dto;

import lombok.Builder;
import stb.core.domain.model.Request;

@Builder
public record RequestDto(
        int id,
        String fullAddress,
        String status,
        boolean urgent
) {

    public static RequestDto toDto(Request model) {
        return RequestDto.builder()
                .id(model.getId())
                .fullAddress(model.getFullAddress())
                .status(model.getStatus().getKeyword().name())
                .urgent(model.isUrgent())
                .build();
    }

}
