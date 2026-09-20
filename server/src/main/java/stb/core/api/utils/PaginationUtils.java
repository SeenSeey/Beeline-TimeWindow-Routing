package stb.core.api.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class PaginationUtils {

    private static final int MIN_PAGE = 1;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 50;

    private PaginationUtils() {}

    public static Pageable create(int page, int limit) {
        int clampedPage = Math.max(page, MIN_PAGE);
        int clampedLimit = Math.min(
                Math.max(limit, MIN_LIMIT),
                MAX_LIMIT
        );

        return PageRequest.of(clampedPage - 1, clampedLimit);
    }
}
