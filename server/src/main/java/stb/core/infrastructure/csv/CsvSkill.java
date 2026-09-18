package stb.core.infrastructure.csv;

import stb.core.domain.model.JobType;

public enum CsvSkill {
    LOCAL_WORK,
    INSTALLATION_OR_ADDON,
    EMERGENCY;

    public static CsvSkill of(JobType.Keyword jobType) {
        return switch (jobType) {
            case LOCAL_WORK -> LOCAL_WORK;
            case INSTALLATION, ADDON -> INSTALLATION_OR_ADDON;
            case EMERGENCY -> EMERGENCY;
        };
    }
}
