package stb.core.api.rest.v1.dto;

import lombok.Builder;
import org.springframework.data.domain.Page;

import java.util.List;

@Builder
public record PageResponseDto<T>(
        List<T> payload,
        int page,
        int limit,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponseDto<T> toDto(Page<T> source) {
        return PageResponseDto.<T>builder()
                .payload(source.getContent())
                .page(source.getNumber() + 1)
                .limit(source.getSize())
                .totalElements(source.getTotalElements())
                .totalPages(source.getTotalPages())
                .build();
    }
}
