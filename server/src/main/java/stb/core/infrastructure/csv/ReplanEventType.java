package stb.core.infrastructure.csv;

/**
 * Значение колонки {@code event_type}. В DTO тип события задаётся самой записью {@code ReplanEventDto}.
 */
public enum ReplanEventType {
    URGENT_REQUEST,
    CANCEL_REQUEST,
    ENGINEER_UNAVAILABLE
}
