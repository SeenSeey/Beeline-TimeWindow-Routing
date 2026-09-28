package stb.core.api.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

public class PaginationUtils {

    private static final int MIN_PAGE = 1;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 50;

    /** a stable order within equal values, so the same row never shows up on two pages */
    private static final String TIE_BREAKER = "id";

    private PaginationUtils() {}

    public static Pageable create(int page, int limit) {
        return PageRequest.of(clampPage(page), clampLimit(limit));
    }

    /**
     * A page sorted by {@code sort} — "{@code <field>,<asc|desc>}", the field one of {@code sortableFields}
     * (API name → entity property path, e.g. "crew" → "assignedCrew.name"). An unknown field or a missing
     * sort leaves the list in id order; the direction defaults to ascending.
     */
    public static Pageable create(int page, int limit, String sort, Map<String, String> sortableFields) {
        return PageRequest.of(clampPage(page), clampLimit(limit), toSort(sort, sortableFields));
    }

    private static Sort toSort(String sort, Map<String, String> sortableFields) {
        Sort byId = Sort.by(Sort.Direction.ASC, TIE_BREAKER);
        if (sort == null || sort.isBlank()) {
            return byId;
        }

        String[] parts = sort.split(",", 2);
        String property = sortableFields.get(parts[0].trim());
        if (property == null) {
            return byId;
        }

        Sort.Direction direction = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Sort bySort = Sort.by(direction, property);
        return property.equals(TIE_BREAKER) ? bySort : bySort.and(byId);
    }

    private static int clampPage(int page) {
        return Math.max(page, MIN_PAGE) - 1;
    }

    private static int clampLimit(int limit) {
        return Math.min(Math.max(limit, MIN_LIMIT), MAX_LIMIT);
    }
}
