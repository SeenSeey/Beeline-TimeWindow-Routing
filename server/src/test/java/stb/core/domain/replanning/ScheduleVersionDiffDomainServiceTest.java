package stb.core.domain.replanning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import stb.core.domain.model.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ScheduleVersionDiffDomainServiceTest {

    private final ScheduleVersionDiffDomainService service = new ScheduleVersionDiffDomainService();
    private final Region region = new Region("Region", "Office", 0, 0, null);
    private final EngineerCrew crew1 = crew(1);
    private final EngineerCrew crew2 = crew(2);
    private Schedule previous;
    private Schedule current;

    @BeforeEach
    void setUp() {
        previous = new Schedule(LocalDate.of(2026, 9, 26), region);
        ReflectionTestUtils.setField(previous, "id", 10);
        current = Schedule.createFrom(previous);
        ReflectionTestUtils.setField(current, "id", 11);
    }

    @Test
    void findsAddedRemovedAndEveryKindOfMovedSlotAndIgnoresUnchanged() {
        Request removed = request(1);
        Request added = request(2);
        Request timeMoved = request(3);
        Request crewMoved = request(4);
        Request combinedMoved = request(5);
        Request unchanged = request(6);
        add(previous, removed, crew1, 9, 10);
        add(previous, timeMoved, crew1, 10, 11);
        add(previous, crewMoved, crew1, 11, 12);
        add(previous, combinedMoved, crew1, 12, 13);
        add(previous, unchanged, crew1, 13, 14);
        add(current, added, crew1, 9, 10);
        add(current, timeMoved, crew1, 10, 12);
        add(current, crewMoved, crew2, 11, 12);
        add(current, combinedMoved, crew2, 14, 15);
        add(current, unchanged, crew1, 13, 14);

        ScheduleVersionDiff diff = service.compare(previous, current);

        assertThat(diff.added()).extracting(slot -> slot.getRequest().getId()).containsExactly(2);
        assertThat(diff.removed()).extracting(slot -> slot.getRequest().getId()).containsExactly(1);
        assertThat(diff.moved()).extracting(change -> change.currentSlot().getRequest().getId())
                .containsExactly(3, 4, 5);
    }

    @Test
    void ignoresNonRequestSlots() {
        current.addSlot(request(1), null, ScheduleSlot.SlotType.TRAVEL,
                LocalTime.NOON, LocalTime.NOON, LocalTime.NOON);

        assertThat(service.compare(previous, current)).satisfies(diff -> {
            assertThat(diff.added()).isEmpty();
            assertThat(diff.removed()).isEmpty();
            assertThat(diff.moved()).isEmpty();
            assertThat(diff.isEmpty()).isTrue();
        });
    }

    @Test
    void rejectsDuplicateRequestInEitherVersion() {
        Request duplicate = request(1);
        add(previous, duplicate, crew1, 9, 10);
        add(previous, duplicate, crew1, 10, 11);

        assertThatIllegalArgumentException().isThrownBy(() -> service.compare(previous, current));
    }

    @Test
    void rejectsUnrelatedVersions() {
        Schedule unrelated = new Schedule(LocalDate.of(2026, 9, 26), region);

        assertThatIllegalArgumentException().isThrownBy(() -> service.compare(previous, unrelated));
    }

    private void add(Schedule schedule, Request request, EngineerCrew crew, int startHour, int endHour) {
        schedule.addSlot(request, crew, ScheduleSlot.SlotType.REQUEST,
                LocalTime.of(startHour, 0), LocalTime.of(startHour, 0), LocalTime.of(endHour, 0));
    }

    private Request request(int id) {
        Request request = Request.builder().build();
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }

    private EngineerCrew crew(int id) {
        EngineerCrew crew = new EngineerCrew("Crew " + id, region, null,
                LocalTime.of(8, 0), LocalTime.of(18, 0), List.of(), List.of());
        ReflectionTestUtils.setField(crew, "id", id);
        return crew;
    }
}
