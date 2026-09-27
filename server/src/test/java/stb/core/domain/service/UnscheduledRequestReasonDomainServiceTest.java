package stb.core.domain.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import stb.core.domain.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnscheduledRequestReasonDomainServiceTest {

    private final UnscheduledRequestReasonDomainService service = new UnscheduledRequestReasonDomainService();
    private final Region region = new Region("Регион", "Офис", 0, 0, null);
    private final Skill requiredSkill = new Skill("Монтаж", Skill.Keyword.INSTALLATION_OR_ADDON);
    private final Skill otherSkill = new Skill("Аварии", Skill.Keyword.EMERGENCY);
    private final Vehicle car = new Vehicle("Автомобиль", Vehicle.Keyword.CAR, 40, 1);
    private final Vehicle bicycle = new Vehicle("Велосипед", Vehicle.Keyword.BICYCLE, 15, 1);
    private final Equipment router = equipment(1, "Роутер");
    private final Equipment tvBox = equipment(2, "ТВ-приставка");

    @Test
    void ignoresAssignedRequestAndCreatesExactlyOneReasonForUnassignedRequest() {
        EngineerCrew crew = crew(10, car, requiredSkill);
        Request assigned = request(1, null, null);
        Request unassigned = request(2, null, null);
        Schedule schedule = initialSchedule();
        addSlot(schedule, assigned, crew);

        List<AlterationReason> result = analyze(schedule, List.of(assigned, unassigned), List.of(crew), false);

        assertThat(result).singleElement().satisfies(reason -> {
            assertThat(reason.getSchedule()).isSameAs(schedule);
            assertThat(reason.getRequest()).isSameAs(unassigned);
            assertThat(reason.getType()).isEqualTo(AlterationReason.Type.NO_AVAILABLE_TIME_SLOT);
        });
    }

    @Test
    void selectsSkillReasonFirstIncludingWhenNoCrewsAreActive() {
        Request request = request(1, car, router);
        EngineerCrew wrongSkillWithRightVehicle = crew(10, car, otherSkill);

        assertThat(reasonFor(request, List.of(wrongSkillWithRightVehicle)))
                .isEqualTo(AlterationReason.Type.NO_CREW_WITH_REQUIRED_SKILL);
        assertThat(reasonFor(request, List.of()))
                .isEqualTo(AlterationReason.Type.NO_CREW_WITH_REQUIRED_SKILL);
    }

    @Test
    void selectsVehicleReasonOnlyAfterSkillFilter() {
        Request request = request(1, car, router);
        EngineerCrew rightSkillWrongVehicle = crew(10, bicycle, requiredSkill);
        EngineerCrew wrongSkillRightVehicle = crew(20, car, otherSkill);

        assertThat(reasonFor(request, List.of(rightSkillWrongVehicle, wrongSkillRightVehicle)))
                .isEqualTo(AlterationReason.Type.NO_CREW_WITH_REQUIRED_VEHICLE);
    }

    @Test
    void requestsWithoutVehicleOrEquipmentProceedToTimeReason() {
        Request request = request(1, null, null);

        assertThat(reasonFor(request, List.of(crew(10, bicycle, requiredSkill))))
                .isEqualTo(AlterationReason.Type.NO_AVAILABLE_TIME_SLOT);
    }

    @Test
    void initialPlanningSelectsCapacityReasonWhenEveryCompatibleCrewRouteIsFull() {
        EngineerCrew crew = crew(10, car, requiredSkill);
        Request scheduledWithEquipment = request(1, null, router);
        Request target = request(2, car, router);
        Schedule schedule = initialSchedule();
        addSlot(schedule, scheduledWithEquipment, crew);

        assertThat(reasonOf(schedule, target, List.of(scheduledWithEquipment, target), List.of(crew), false))
                .isEqualTo(AlterationReason.Type.NO_CAPACITY_FOR_REQUIRED_EQUIPMENT);
    }

    @Test
    void initialPlanningSelectsTimeReasonWhenAnyCompatibleCrewHasEquipmentCapacity() {
        EngineerCrew fullCrew = crew(10, car, requiredSkill);
        EngineerCrew freeCrew = crew(20, car, requiredSkill);
        Request scheduledWithEquipment = request(1, null, router);
        Request target = request(2, car, router);
        Schedule schedule = initialSchedule();
        addSlot(schedule, scheduledWithEquipment, fullCrew);

        assertThat(reasonOf(schedule, target, List.of(scheduledWithEquipment, target), List.of(fullCrew, freeCrew), false))
                .isEqualTo(AlterationReason.Type.NO_AVAILABLE_TIME_SLOT);
    }

    @Test
    void replanningSelectsStockReasonWhenCompatibleCrewHasNoRequiredEquipment() {
        EngineerCrew crewWithOtherEquipment = crew(10, car, requiredSkill);
        stock(crewWithOtherEquipment, tvBox, 1);
        Request target = request(2, car, router);

        assertThat(reasonOf(replanSchedule(), target, List.of(target), List.of(crewWithOtherEquipment), true))
                .isEqualTo(AlterationReason.Type.NO_REQUIRED_EQUIPMENT_IN_STOCK);
    }

    @Test
    void replanningSelectsStockReasonWhenStockIsTakenByConsideredRequests() {
        EngineerCrew crew = crew(10, car, requiredSkill);
        stock(crew, router, 1);
        Request scheduledWithRouter = request(1, null, router);
        Request target = request(2, car, router);
        Schedule schedule = replanSchedule();
        addSlot(schedule, scheduledWithRouter, crew);

        assertThat(reasonOf(schedule, target, List.of(scheduledWithRouter, target), List.of(crew), true))
                .isEqualTo(AlterationReason.Type.NO_REQUIRED_EQUIPMENT_IN_STOCK);
    }

    @Test
    void replanningSelectsTimeReasonWhenAnyCompatibleCrewHasEquipmentInStock() {
        EngineerCrew emptyCrew = crew(10, car, requiredSkill);
        EngineerCrew stockedCrew = crew(20, car, requiredSkill);
        stock(stockedCrew, router, 1);
        Request target = request(2, car, router);

        assertThat(reasonOf(replanSchedule(), target, List.of(target), List.of(emptyCrew, stockedCrew), true))
                .isEqualTo(AlterationReason.Type.NO_AVAILABLE_TIME_SLOT);
    }

    @Test
    void replanningIgnoresStaticSlotsWhenCountingStock() {
        EngineerCrew crew = crew(10, car, requiredSkill);
        stock(crew, router, 1);
        Request inProgressWithRouter = request(1, null, router);
        Request target = request(2, car, router);
        Schedule schedule = replanSchedule();
        addSlot(schedule, inProgressWithRouter, crew);

        assertThat(reasonOf(schedule, target, List.of(target), List.of(crew), true))
                .isEqualTo(AlterationReason.Type.NO_AVAILABLE_TIME_SLOT);
    }

    @Test
    void equipmentRuleFollowsReplanningFlagNotScheduleVersion() {
        EngineerCrew crewWithoutStock = crew(10, car, requiredSkill);
        Request target = request(2, car, router);

        assertThat(reasonOf(replanSchedule(), target, List.of(target), List.of(crewWithoutStock), false))
                .isEqualTo(AlterationReason.Type.NO_AVAILABLE_TIME_SLOT);
    }

    @Test
    void replanningSkipsRequestRemovedFromPreviousVersion() {
        EngineerCrew crew = crew(10, car, requiredSkill);
        Request removed = request(1, null, null);
        Schedule previous = initialSchedule();
        addSlot(previous, removed, crew);
        Schedule current = Schedule.createFrom(previous);

        assertThat(analyze(current, List.of(removed), List.of(crew), true)).isEmpty();
    }

    @Test
    void replanningClassifiesAgainRequestThatStaysOutOfSchedule() {
        EngineerCrew crew = crew(10, car, requiredSkill);
        Request stillUnscheduled = request(1, null, null);
        Schedule previous = initialSchedule();
        Schedule current = Schedule.createFrom(previous);

        assertThat(analyze(current, List.of(stillUnscheduled), List.of(crew), true)).singleElement().satisfies(reason -> {
            assertThat(reason.getSchedule()).isSameAs(current);
            assertThat(reason.getRequest()).isSameAs(stillUnscheduled);
            assertThat(reason.getType()).isEqualTo(AlterationReason.Type.NO_AVAILABLE_TIME_SLOT);
        });
    }

    private AlterationReason.Type reasonFor(Request request, List<EngineerCrew> crews) {
        return reasonOf(initialSchedule(), request, List.of(request), crews, false);
    }

    private AlterationReason.Type reasonOf(Schedule schedule,
                                           Request target,
                                           List<Request> requests,
                                           List<EngineerCrew> crews,
                                           boolean replanning) {
        return analyze(schedule, requests, crews, replanning).stream()
                .filter(reason -> reason.getRequest() == target)
                .findFirst()
                .orElseThrow()
                .getType();
    }

    private List<AlterationReason> analyze(Schedule schedule,
                                           List<Request> requests,
                                           List<EngineerCrew> crews,
                                           boolean replanning) {
        return service.analyze(schedule, requests, crews, replanning, LocalDateTime.of(2026, 9, 27, 12, 0));
    }

    private Schedule initialSchedule() {
        return new Schedule(LocalDate.of(2026, 9, 27), region);
    }

    private Schedule replanSchedule() {
        return Schedule.createFrom(initialSchedule());
    }

    private static void addSlot(Schedule schedule, Request request, EngineerCrew crew) {
        schedule.addSlot(request, crew, ScheduleSlot.SlotType.REQUEST,
                LocalTime.of(9, 0), LocalTime.of(9, 0), LocalTime.of(10, 0));
    }

    private static void stock(EngineerCrew crew, Equipment equipment, int quantity) {
        crew.getEquipment().add(new EngineerCrewEquipment(crew, equipment, quantity));
    }

    private static Equipment equipment(int id, String name) {
        Equipment equipment = new Equipment(name);
        ReflectionTestUtils.setField(equipment, "id", id);
        return equipment;
    }

    private EngineerCrew crew(int id, Vehicle vehicle, Skill skill) {
        EngineerCrew crew = new EngineerCrew("Бригада " + id, region, vehicle,
                LocalTime.of(8, 0), LocalTime.of(18, 0), List.of(skill), new ArrayList<>());
        ReflectionTestUtils.setField(crew, "id", id);
        return crew;
    }

    private Request request(int id, Vehicle requiredVehicle, Equipment requiredEquipment) {
        Request request = Request.builder()
                .jobType(new JobType("Монтаж", List.of(requiredSkill), 30, 60, 90,
                        JobType.Keyword.INSTALLATION, 3))
                .requiredVehicle(requiredVehicle)
                .requiredEquipment(requiredEquipment)
                .startTimeWindow(LocalTime.of(12, 0))
                .endTimeWindow(LocalTime.of(14, 0))
                .build();
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }
}
