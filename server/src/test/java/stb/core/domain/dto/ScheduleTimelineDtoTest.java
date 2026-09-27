package stb.core.domain.dto;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import stb.core.domain.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleTimelineDtoTest {

    @Test
    void groupsSlotsByCrewSnapshotInsteadOfCurrentRequestAssignment() {
        Region region = new Region("Region", "Office", 0, 0, null);
        EngineerCrew oldCrew = crew("Old crew", region, 10);
        EngineerCrew newCrew = crew("New crew", region, 20);
        Request request = Request.builder()
                .fullAddress("Address")
                .region(region)
                .jobType(new JobType("Job", List.of(), 1, 1, 1, JobType.Keyword.LOCAL_WORK, 1))
                .deadline(LocalDateTime.of(2026, 9, 26, 18, 0))
                .createdAt(LocalDateTime.of(2026, 9, 26, 8, 0))
                .startTimeWindow(LocalTime.of(8, 0))
                .endTimeWindow(LocalTime.of(18, 0))
                .build();
        ReflectionTestUtils.setField(request, "id", 42);
        Schedule schedule = new Schedule(LocalDate.of(2026, 9, 26), region);
        schedule.addSlot(request, oldCrew, ScheduleSlot.SlotType.REQUEST,
                LocalTime.of(8, 50), LocalTime.of(9, 0), LocalTime.of(10, 0));

        request.assignCrew(newCrew, List.of(new RequestStatus("Assigned", RequestStatus.Keyword.ASSIGNED)));

        assertThat(ScheduleTimelineDto.EngineerCrewRow.listFrom(schedule.getSlots()))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.id()).isEqualTo(10);
                    assertThat(row.name()).isEqualTo("Old crew");
                });
    }

    private static EngineerCrew crew(String name, Region region, int id) {
        EngineerCrew crew = new EngineerCrew(name, region, null,
                LocalTime.of(8, 0), LocalTime.of(18, 0), List.of(), List.of());
        ReflectionTestUtils.setField(crew, "id", id);
        return crew;
    }
}
