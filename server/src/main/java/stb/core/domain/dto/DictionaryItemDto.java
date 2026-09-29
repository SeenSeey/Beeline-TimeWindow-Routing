package stb.core.domain.dto;

import stb.core.domain.model.*;

/**
 * An entry of a dictionary for selectors and filters: its database id (what filters and forms send back),
 * its keyword (the enum name, for logic and styling; null for equipment, which has none) and its display name
 */
public record DictionaryItemDto(int id, String keyword, String displayName) {

    public static DictionaryItemDto from(RequestStatus status) {
        return new DictionaryItemDto(status.getId(), status.getKeyword().name(), status.getDisplayName());
    }

    public static DictionaryItemDto from(JobType jobType) {
        return new DictionaryItemDto(jobType.getId(), jobType.getKeyword().name(), jobType.getDisplayName());
    }

    public static DictionaryItemDto from(Vehicle vehicle) {
        return new DictionaryItemDto(vehicle.getId(), vehicle.getKeyword().name(), vehicle.getDisplayName());
    }

    public static DictionaryItemDto from(Skill skill) {
        return new DictionaryItemDto(skill.getId(), skill.getKeyword().name(), skill.getDisplayName());
    }

    public static DictionaryItemDto from(Equipment equipment) {
        return new DictionaryItemDto(equipment.getId(), null, equipment.getDisplayName());
    }
}
