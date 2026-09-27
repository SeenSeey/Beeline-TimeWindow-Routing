package stb.core.domain.dto;

import stb.core.domain.model.AlterationReason;

public record AlterationReasonDto(int requestId, String description) {

    public static AlterationReasonDto from(AlterationReason reason, String description) {
        return new AlterationReasonDto(reason.getRequest().getId(), description);
    }
}
