package stb.core.domain.dto.replanning;

public enum RequestChangeType {
    ADDED_TO_PLAN,
    ADDED_AS_UNASSIGNED,
    REMOVED_FROM_PLAN,
    ASSIGNED_FROM_UNASSIGNED,
    BECAME_UNASSIGNED,
    REASSIGNED,
    ORDER_CHANGED,
    PLANNED_TIME_CHANGED
}
