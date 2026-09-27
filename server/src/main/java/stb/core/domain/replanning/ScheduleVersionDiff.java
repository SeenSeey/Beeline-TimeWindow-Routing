package stb.core.domain.replanning;

import stb.core.domain.model.ScheduleSlot;

import java.util.List;

public record ScheduleVersionDiff(
        List<ScheduleSlot> added,
        List<ScheduleSlot> removed,
        List<MovedSlot> moved
) {

    public ScheduleVersionDiff {
        added = List.copyOf(added);
        removed = List.copyOf(removed);
        moved = List.copyOf(moved);
    }

    public boolean isEmpty() {
        return added.isEmpty() && removed.isEmpty() && moved.isEmpty();
    }

    public record MovedSlot(ScheduleSlot previousSlot, ScheduleSlot currentSlot) {
    }
}
