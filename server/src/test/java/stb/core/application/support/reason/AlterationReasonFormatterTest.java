package stb.core.application.support.reason;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import stb.core.domain.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlterationReasonFormatterTest {

    private final AlterationReasonFormatter formatter = new AlterationReasonFormatter();
    private final Region region = new Region("Регион", "Офис", 0, 0, null);
    private final Schedule schedule = new Schedule(LocalDate.of(2026, 9, 27), region);
    private final Skill skill = new Skill("Монтаж", Skill.Keyword.INSTALLATION_OR_ADDON);
    private final Vehicle vehicle = new Vehicle("Автомобиль", Vehicle.Keyword.CAR, 40, 1);
    private final Equipment equipment = new Equipment("Роутер");
    private final Request request = Request.builder()
            .jobType(new JobType("Монтаж", List.of(skill), 30, 60, 90, JobType.Keyword.INSTALLATION, 3))
            .requiredVehicle(vehicle)
            .requiredEquipment(equipment)
            .startTimeWindow(LocalTime.of(12, 0))
            .endTimeWindow(LocalTime.of(14, 0))
            .build();
    private final Request causeRequest = causeRequest();
    private final EngineerCrew causeCrew = new EngineerCrew("B", region, vehicle,
            LocalTime.of(8, 0), LocalTime.of(18, 0), List.of(skill), List.of());

    @Test
    void formatsEveryType() {
        for (AlterationReason.Type type : AlterationReason.Type.values()) {
            String description = formatter.format(reason(type));

            switch (type.getCause()) {
                case REQUEST -> assertThat(description).contains("№42");
                case CREW -> assertThat(description).contains("«B»");
                case NONE -> assertThat(description).isNotBlank();
            }
        }
    }

    @Test
    void formatsStructuredRequestFieldsForUnscheduledTypes() {
        assertThat(format(AlterationReason.Type.NO_CREW_WITH_REQUIRED_SKILL)).contains("Монтаж");
        assertThat(format(AlterationReason.Type.NO_CREW_WITH_REQUIRED_VEHICLE)).contains("Автомобиль");
        assertThat(format(AlterationReason.Type.NO_CAPACITY_FOR_REQUIRED_EQUIPMENT))
                .contains("Роутер", "вместимость");
        assertThat(format(AlterationReason.Type.NO_REQUIRED_EQUIPMENT_IN_STOCK))
                .isEqualTo("У подходящих бригад нет в наличии оборудования «Роутер»");
        assertThat(format(AlterationReason.Type.NO_AVAILABLE_TIME_SLOT)).contains("12:00–14:00");
    }

    @Test
    void formatsTransitionTypesFromStructuredCause() {
        assertThat(format(AlterationReason.Type.MOVED_AFTER_REQUEST_CANCELLATION))
                .isEqualTo("Заявка перенесена после отмены заявки №42");
        assertThat(format(AlterationReason.Type.MOVED_FOR_CREW_OFF_SHIFT))
                .isEqualTo("Заявка перераспределена, так как бригада «B» недоступна");
    }

    private String format(AlterationReason.Type type) {
        return formatter.format(reason(type));
    }

    private AlterationReason reason(AlterationReason.Type type) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 27, 12, 0);
        return switch (type.getCause()) {
            case REQUEST -> AlterationReason.causedByRequest(schedule, request, type, causeRequest, createdAt);
            case CREW -> AlterationReason.causedByCrew(schedule, request, type, causeCrew, createdAt);
            case NONE -> AlterationReason.unscheduled(schedule, request, type, createdAt);
        };
    }

    private static Request causeRequest() {
        Request cause = Request.builder().build();
        ReflectionTestUtils.setField(cause, "id", 42);
        return cause;
    }
}
