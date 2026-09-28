package stb.core.infrastructure.csv;

public enum EngineerColumn implements CsvColumn {
    ID,
    NAME,
    START_ADDRESS,
    START_LATITUDE,
    START_LONGITUDE,
    SHIFT_START,
    SHIFT_END,
    SKILLS,
    VEHICLE,
    DISTRICT(true);

    private final boolean optional;

    EngineerColumn() {
        this(false);
    }

    EngineerColumn(boolean optional) {
        this.optional = optional;
    }

    @Override
    public boolean optional() {
        return optional;
    }
}
