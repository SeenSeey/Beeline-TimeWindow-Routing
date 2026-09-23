package stb.core.domain.dto;

import stb.core.domain.model.*;

public record DictionaryDto(String keyword, String displayName) {
    public static DictionaryDto from(JobType jobType) {
        return new DictionaryDto(jobType.getKeyword().name(), jobType.getDisplayName());
    }

    public static DictionaryDto from(RequestStatus status) {
        return new DictionaryDto(status.getKeyword().name(), status.getDisplayName());
    }

    public static DictionaryDto from(Vehicle vehicle) {
        return new DictionaryDto(vehicle.getKeyword().name(), vehicle.getDisplayName());
    }

    public static DictionaryDto from(Skill skill) {
        return new DictionaryDto(skill.getKeyword().name(), skill.getDisplayName());
    }

    public static DictionaryDto from(Schedule.Status status) {
        return new DictionaryDto(status.name(), status.getDisplayName());
    }
}
