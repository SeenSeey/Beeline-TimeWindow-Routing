package stb.core.infrastructure.csv;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import stb.core.domain.model.EngineerCrew;
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

    public Region toRegion(String displayName, EngineerDto anyEngineer) {
        return new Region(
                displayName,
                anyEngineer.startAddress(),
                anyEngineer.start().latitude(),
                anyEngineer.start().longitude(),
                null);
    }

    public EngineerCrew toCrew(EngineerDto engineer, Region region, Map<Vehicle.Keyword, Vehicle> vehicles,
            Map<Skill.Keyword, Skill> skills) {
        return new EngineerCrew(
                engineer.name(),
                region,
                vehicles.get(engineer.vehicle()),
                engineer.shiftStart(),
                engineer.shiftEnd(),
                engineer.skills().stream().map(skill -> skills.get(Skill.Keyword.valueOf(skill.name()))).toList(),
                List.of());
    }

    public Request toRequest(RequestDto request, Region region, Map<JobType.Keyword, JobType> jobTypes,
            Map<Vehicle.Keyword, Vehicle> vehicles, RequestStatus status, LocalDateTime createdAt) {
        return Request.builder()
                .fullAddress(request.address())
                .latitude(request.location().latitude())
                .longitude(request.location().longitude())
                .status(status)
                .urgent(false)
                .jobType(jobTypes.get(request.jobType()))
                .region(region)
                .startTimeWindow(request.windowStart())
                .endTimeWindow(request.windowEnd())
                .requiredVehicle(request.requiredVehicle() == null ? null : vehicles.get(request.requiredVehicle()))
                .equipment(List.of())
                .createdAt(createdAt)
                .build();
    }
}
