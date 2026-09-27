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
    private final EngineerCrew previousCrew = new EngineerCrew("P", region, vehicle,
            LocalTime.of(8, 0), LocalTime.of(18, 0), List.of(skill), List.of());
    private final ScheduleSlot previousSlot = new ScheduleSlot(schedule, request, previousCrew,
            ScheduleSlot.SlotType.REQUEST, LocalTime.of(9, 20), LocalTime.of(9, 30), LocalTime.of(10, 30));

    @Test
    void formatsEveryType() {
        for (AlterationReason.Type type : AlterationReason.Type.values()) {
            String description = formatter.format(reason(type));

            switch (type.getCause()) {
                case REQUEST -> assertThat(description).contains("№42");
                case CREW -> assertThat(description).contains("«B»");
                case NONE -> assertThat(description).isNotBlank();
            }
            if (type.isMovement()) {
                assertThat(description).containsAnyOf("«P»", "09:30");
            }
        }
    }

    @Test
    void formatsStructuredRequestFieldsForUnscheduledTypes() {
        assertThat(format(AlterationReason.Type.NO_CREW_WITH_REQUIRED_SKILL))
                .isEqualTo("Среди бригад на смене нет бригады с навыком «Монтаж»");
        assertThat(format(AlterationReason.Type.NO_CREW_WITH_REQUIRED_VEHICLE))
                .isEqualTo("Среди бригад с нужными навыками нет бригады с транспортом «Автомобиль»");
        assertThat(format(AlterationReason.Type.NO_CAPACITY_FOR_REQUIRED_EQUIPMENT))
                .isEqualTo("Подходящие бригады не могут взять оборудование «Роутер»: "
                        + "транспорт уже заполнен оборудованием для других заявок");
        assertThat(format(AlterationReason.Type.NO_REQUIRED_EQUIPMENT_IN_STOCK))
                .isEqualTo("У подходящих бригад не осталось свободного оборудования «Роутер»");
        assertThat(format(AlterationReason.Type.NO_AVAILABLE_TIME_SLOT))
                .isEqualTo("У подходящих бригад не нашлось свободного времени в окне визита 12:00–14:00");
    }

    @Test
    void skillReasonSaysAnyOfSkillsIsEnoughAndExplainsMissingSkills() {
        Skill otherSkill = new Skill("Аварии", Skill.Keyword.EMERGENCY);

        assertThat(skillReason(new JobType("Монтаж", List.of(skill, otherSkill), 30, 60, 90, JobType.Keyword.INSTALLATION, 3)))
                .isEqualTo("Среди бригад на смене нет бригады ни с одним из навыков: «Монтаж», «Аварии»");
        assertThat(skillReason(new JobType("Монтаж", List.of(), 30, 60, 90, JobType.Keyword.INSTALLATION, 3)))
                .isEqualTo("У типа работ «Монтаж» не указаны навыки, поэтому ни одна бригада не подходит");
    }

    @Test
    void formatsMovementTypesFromPreviousPosition() {
        assertThat(format(AlterationReason.Type.REASSIGNED_FOR_CREW_OFF_SHIFT))
                .isEqualTo("Заявка передана другой бригаде: бригада «B» снята со смены");
        assertThat(format(AlterationReason.Type.REASSIGNED))
                .isEqualTo("Заявка передана от бригады «P» при перестроении маршрутов");
        assertThat(format(AlterationReason.Type.RESCHEDULED_AFTER_INSERTION))
                .isEqualTo("Визит перенесён на более позднее время (вместо 09:30): "
                        + "в маршрут бригады перед этой заявкой добавили заявку №42");
        assertThat(format(AlterationReason.Type.RESCHEDULED_AFTER_REMOVAL))
                .isEqualTo("Визит перенесён на более раннее время (вместо 09:30): "
                        + "из маршрута бригады убрали заявку №42, которая стояла перед этой");
        assertThat(format(AlterationReason.Type.RESCHEDULED))
                .isEqualTo("Визит перенесён (вместо 09:30) при перестроении маршрута бригады");
    }

    private String skillReason(JobType jobType) {
        Request skillRequest = Request.builder().jobType(jobType).build();
        return formatter.format(AlterationReason.unscheduled(schedule, skillRequest,
                AlterationReason.Type.NO_CREW_WITH_REQUIRED_SKILL, LocalDateTime.of(2026, 9, 27, 12, 0)));
    }

    private String format(AlterationReason.Type type) {
        return formatter.format(reason(type));
    }

    private AlterationReason reason(AlterationReason.Type type) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 27, 12, 0);
        if (type.isMovement()) {
            return type.getCause() == AlterationReason.Cause.REQUEST
                    ? AlterationReason.movedByRequest(schedule, previousSlot, type, causeRequest, createdAt)
                    : AlterationReason.moved(schedule, previousSlot, type, createdAt);
        }
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
