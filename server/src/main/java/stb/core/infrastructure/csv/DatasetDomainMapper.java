package stb.core.infrastructure.csv;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

import org.springframework.stereotype.Component;

import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Equipment;
import stb.core.domain.model.JobType;
import stb.core.domain.model.Region;
import stb.core.domain.model.Request;
import stb.core.domain.model.RequestStatus;
import stb.core.domain.model.Skill;
import stb.core.domain.model.Vehicle;
import stb.core.infrastructure.csv.dto.EngineerDto;
import stb.core.infrastructure.csv.dto.RequestDto;

@Component
public class DatasetDomainMapper {

    public static final Map<CsvEquipment, String> EQUIPMENT_NAMES = Map.of(
            CsvEquipment.ROUTER, "Роутер",
            CsvEquipment.SET_TOP_BOX, "ТВ-приставка",
            CsvEquipment.SMART_SPEAKER, "Умная колонка");

    private static final Map<JobType.Keyword, Integer> DEADLINE_HOURS = Map.of(
            JobType.Keyword.EMERGENCY, 24,
            JobType.Keyword.INSTALLATION, 72,
            JobType.Keyword.ADDON, 24 * 7,
            JobType.Keyword.LOCAL_WORK, 24 * 7);

    public Region toRegion(String displayName, EngineerDto anyEngineer) {
        return new Region(
                displayName,
                anyEngineer.startAddress(),
                anyEngineer.start().latitude(),
                anyEngineer.start().longitude(),
                null);
    }

    public Region toDistrict(EngineerDto anyDistrictEngineer, Region parentRegion) {
        return new Region(
                anyDistrictEngineer.district(),
                anyDistrictEngineer.startAddress(),
                anyDistrictEngineer.start().latitude(),
                anyDistrictEngineer.start().longitude(),
                parentRegion);
    }

    public EngineerCrew toCrew(EngineerDto engineer,
                               Region region,
                               Map<Vehicle.Keyword, Vehicle> vehicles,
                               Map<Skill.Keyword, Skill> skills) {

        return new EngineerCrew(
                engineer.name(),
                region,
                vehicles.get(engineer.vehicle()),
                engineer.shiftStart(),
                engineer.shiftEnd(),
                engineer.skills().stream().map(skill -> skills.get(Skill.Keyword.valueOf(skill.name()))).toList(),
                new ArrayList<>());
    }

    public Request toRequest(RequestDto request, Region region, Map<JobType.Keyword, JobType> jobTypes,
            Map<Vehicle.Keyword, Vehicle> vehicles, Map<CsvEquipment, Equipment> equipment, RequestStatus status,
            LocalDateTime createdAt) {
        return Request.builder()
                .id(request.id())
                .fullAddress(request.address())
                .latitude(request.location().latitude())
                .longitude(request.location().longitude())
                .status(status)
                .urgent(request.jobType() == JobType.Keyword.EMERGENCY)
                .deadline(deadline(request, createdAt))
                .jobType(jobTypes.get(request.jobType()))
                .region(region)
                .startTimeWindow(request.windowStart())
                .endTimeWindow(request.windowEnd())
                .requiredVehicle(request.requiredVehicle() == null ? null : vehicles.get(request.requiredVehicle()))
                .requiredEquipment(request.requiredEquipment() == null ? null : equipment.get(request.requiredEquipment()))
                .createdAt(createdAt)
                .build();
    }

    private LocalDateTime deadline(RequestDto request, LocalDateTime createdAt) {
        return createdAt.plusHours(DEADLINE_HOURS.get(request.jobType()));
    }
}
