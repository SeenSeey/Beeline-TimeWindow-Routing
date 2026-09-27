package stb.core.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ScheduleSlotTest {

    @Test
    void requestSlotRequiresCrewAndCopyKeepsSnapshot() {
        Region region = new Region("Region", "Office", 0, 0, null);
        Schedule previous = new Schedule(LocalDate.of(2026, 9, 26), region);
        Schedule current = Schedule.createFrom(previous);
        Request request = Request.builder().build();
        EngineerCrew oldCrew = new EngineerCrew();
        EngineerCrew newCrew = new EngineerCrew();
        ScheduleSlot slot = new ScheduleSlot(previous, request, oldCrew, ScheduleSlot.SlotType.REQUEST,
                LocalTime.of(8, 50), LocalTime.of(9, 0), LocalTime.of(10, 0));

        request.assignCrew(newCrew, java.util.List.of(new RequestStatus("Assigned", RequestStatus.Keyword.ASSIGNED)));
        ScheduleSlot copy = ScheduleSlot.createFrom(current, slot);

        assertThat(slot.getEngineerCrew()).isSameAs(oldCrew);
        assertThat(copy.getEngineerCrew()).isSameAs(oldCrew);
        assertThat(request.getAssignedCrew()).isSameAs(newCrew);
        assertThatNullPointerException().isThrownBy(() -> new ScheduleSlot(
                previous, request, null, ScheduleSlot.SlotType.REQUEST,
                LocalTime.NOON, LocalTime.NOON, LocalTime.of(13, 0)));
    }
}
