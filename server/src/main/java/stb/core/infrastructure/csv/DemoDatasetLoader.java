package stb.core.infrastructure.csv;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Equipment;
import stb.core.domain.model.JobType;
import stb.core.domain.model.Region;
import stb.core.domain.model.Request;
import stb.core.domain.model.RequestStatus;
import stb.core.domain.model.Skill;
import stb.core.domain.model.Vehicle;
import stb.core.domain.repository.EngineerCrewRepository;
import stb.core.domain.repository.EquipmentRepository;
import stb.core.domain.repository.JobTypeRepository;
import stb.core.domain.repository.RegionRepository;
import stb.core.domain.repository.RequestRepository;
import stb.core.domain.repository.RequestStatusRepository;
import stb.core.domain.repository.SkillRepository;
import stb.core.domain.repository.VehicleRepository;
import stb.core.infrastructure.csv.dto.DatasetDto;

@Component
@RequiredArgsConstructor
public class DemoDatasetLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDatasetLoader.class);

    private static final Map<String, String> REGION_NAMES = Map.of(
            "vostok", "Восток",
            "yugo-vostok", "Юго-восток",
            "yugo-centr", "Югоцентр");

    private static final List<String> REGION_DIRS = List.of("vostok", "yugo-vostok", "yugo-centr");

    private final CsvDatasetParser parser = new CsvDatasetParser();

    private final DatasetDomainMapper mapper;
    private final RegionRepository regionRepository;
    private final SkillRepository skillRepository;
    private final JobTypeRepository jobTypeRepository;
    private final VehicleRepository vehicleRepository;
    private final RequestStatusRepository requestStatusRepository;
    private final EquipmentRepository equipmentRepository;
    private final EngineerCrewRepository engineerCrewRepository;
    private final RequestRepository requestRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (regionRepository.count() > 0) {
            log.info("Демо-датасет уже загружен, пропускаю");
            return;
        }

        Map<Skill.Keyword, Skill> skills = skills();
        Map<JobType.Keyword, JobType> jobTypes = jobTypes(skills);
        Map<Vehicle.Keyword, Vehicle> vehicles = vehicles();
        Map<RequestStatus.Keyword, RequestStatus> statuses = statuses();
        Map<CsvEquipment, Equipment> equipment = equipment();
        LocalDateTime createdAt = LocalDateTime.now();

        List<EngineerCrew> crews = new ArrayList<>();
        List<Request> requests = new ArrayList<>();
        for (String dir : REGION_DIRS) {
            DatasetDto dataset = parser.parse(file -> getClass().getResourceAsStream("/demo/dataset/" + dir + "/" + file.fileName()));
            Region region = regionRepository.save(mapper.toRegion(REGION_NAMES.get(dir), dataset.engineers().getFirst()));
            dataset.engineers().forEach(engineer -> crews.add(mapper.toCrew(engineer, region, vehicles, skills)));
            dataset.requests().forEach(request -> requests.add(mapper.toRequest(
                    request, region, jobTypes, vehicles, equipment, statuses.get(RequestStatus.Keyword.IDLE), createdAt)));
        }
        engineerCrewRepository.saveAll(crews);
        requestRepository.saveAll(requests);

        log.info("Демо-датасет загружен: регионов {}, бригад {}, заявок {}",
                REGION_DIRS.size(), crews.size(), requests.size());
    }

    private Map<CsvEquipment, Equipment> equipment() {
        Map<CsvEquipment, Equipment> equipment = new EnumMap<>(CsvEquipment.class);
        DatasetDomainMapper.EQUIPMENT_NAMES.forEach((keyword, name) -> equipment.put(keyword, new Equipment(name)));
        equipmentRepository.saveAll(equipment.values());
        return equipment;
    }

    private Map<Skill.Keyword, Skill> skills() {
        Map<Skill.Keyword, Skill> skills = new EnumMap<>(Skill.Keyword.class);
        skills.put(Skill.Keyword.LOCAL_WORK, new Skill("Локальные работы", Skill.Keyword.LOCAL_WORK));
        skills.put(Skill.Keyword.INSTALLATION_OR_ADDON, new Skill("Подключения и дозаказы", Skill.Keyword.INSTALLATION_OR_ADDON));
        skills.put(Skill.Keyword.EMERGENCY, new Skill("Аварийные работы", Skill.Keyword.EMERGENCY));
        skillRepository.saveAll(skills.values());
        return skills;
    }

    private Map<JobType.Keyword, JobType> jobTypes(Map<Skill.Keyword, Skill> skills) {
        Map<JobType.Keyword, JobType> jobTypes = new EnumMap<>(JobType.Keyword.class);
        jobTypes.put(JobType.Keyword.LOCAL_WORK, new JobType("Локальная заявка",
                List.of(skills.get(Skill.Keyword.LOCAL_WORK)), 20, 30, 50, JobType.Keyword.LOCAL_WORK));
        jobTypes.put(JobType.Keyword.INSTALLATION, new JobType("Подключение",
                List.of(skills.get(Skill.Keyword.INSTALLATION_OR_ADDON)), 20, 60, 90, JobType.Keyword.INSTALLATION));
        jobTypes.put(JobType.Keyword.ADDON, new JobType("Дозаказ оборудования",
                List.of(skills.get(Skill.Keyword.INSTALLATION_OR_ADDON)), 20, 10, 40, JobType.Keyword.ADDON));
        jobTypes.put(JobType.Keyword.EMERGENCY, new JobType("Глобальная проблема",
                List.of(skills.get(Skill.Keyword.EMERGENCY)), 20, 80, 100, JobType.Keyword.EMERGENCY));
        jobTypeRepository.saveAll(jobTypes.values());
        return jobTypes;
    }

    private Map<Vehicle.Keyword, Vehicle> vehicles() {
        Map<Vehicle.Keyword, Vehicle> vehicles = new EnumMap<>(Vehicle.Keyword.class);
        vehicles.put(Vehicle.Keyword.PEDESTRIAN, new Vehicle("Пешком", Vehicle.Keyword.PEDESTRIAN));
        vehicles.put(Vehicle.Keyword.BICYCLE, new Vehicle("Велосипед", Vehicle.Keyword.BICYCLE));
        vehicles.put(Vehicle.Keyword.PUBLIC_TRANSPORT, new Vehicle("Общественный транспорт", Vehicle.Keyword.PUBLIC_TRANSPORT));
        vehicles.put(Vehicle.Keyword.CAR, new Vehicle("Автомобиль", Vehicle.Keyword.CAR));
        vehicleRepository.saveAll(vehicles.values());
        return vehicles;
    }

    private Map<RequestStatus.Keyword, RequestStatus> statuses() {
        Map<RequestStatus.Keyword, RequestStatus> statuses = new EnumMap<>(RequestStatus.Keyword.class);
        statuses.put(RequestStatus.Keyword.IDLE, new RequestStatus("Не назначена", RequestStatus.Keyword.IDLE));
        statuses.put(RequestStatus.Keyword.ASSIGNED, new RequestStatus("Назначена", RequestStatus.Keyword.ASSIGNED));
        statuses.put(RequestStatus.Keyword.IN_PROGRESS, new RequestStatus("В работе", RequestStatus.Keyword.IN_PROGRESS));
        statuses.put(RequestStatus.Keyword.COMPLETED, new RequestStatus("Выполнена", RequestStatus.Keyword.COMPLETED));
        statuses.put(RequestStatus.Keyword.CANCELED, new RequestStatus("Отменена", RequestStatus.Keyword.CANCELED));
        requestStatusRepository.saveAll(statuses.values());
        return statuses;
    }
}
