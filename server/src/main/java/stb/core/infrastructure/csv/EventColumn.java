package stb.core.infrastructure.csv;

public enum EventColumn implements CsvColumn {
    EVENT_TYPE,
    EVENT_TIME,
    REQUEST_ID,
    ENGINEER_ID;

    public static final String REQUEST_PREFIX = "request_";
}
