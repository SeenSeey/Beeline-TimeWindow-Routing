package stb.core.infrastructure.csv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.model.*;
import stb.core.domain.repository.*;
import stb.core.domain.service.RegionDomainService;
import stb.core.infrastructure.csv.dto.DatasetDto;
import stb.core.infrastructure.csv.dto.EngineerDto;
import stb.core.infrastructure.csv.dto.RequestDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatasetLoader {

    private static final Map<String, String> REGION_NAMES = Map.of(
            "vostok", "Восток",
            "yugo-vostok", "Юго-восток",
            "yugo-centr", "Югоцентр");

    private static final double PEDESTRIAN_SPEED_KMH = 5;
    private static final double BICYCLE_SPEED_KMH = 18;
    private static final double PUBLIC_TRANSPORT_SPEED_KMH = 38;
    private static final double CAR_SPEED_KMH = 60;

    private static final int PEDESTRIAN_EQUIPMENT_CAPACITY = 4;
    private static final int BICYCLE_EQUIPMENT_CAPACITY = 6;
    private static final int PUBLIC_TRANSPORT_EQUIPMENT_CAPACITY = 4;
    private static final int CAR_EQUIPMENT_CAPACITY = 100;

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
    private final RequestActionAuditRepository requestActionAuditRepository;
    private final SeasonRepository seasonRepository;
    private final TimePeriodRepository timePeriodRepository;
    private final RegionDomainService regionDomainService;

    @Transactional
    public void loadDataFromCsv() {
        if (seasonRepository.count() == 0) {
            seasonRepository.saveAll(List.of(
                    new Season("Spring", new BigDecimal("1.000"), 4, 5),
                    new Season("Autumn", new BigDecimal("1.000"), 9, 10, 11),
                    new Season("Summer", new BigDecimal("0.875"), 6, 7, 8),
                    new Season("Winter", new BigDecimal("1.250"), 12, 1, 2, 3)
            ));
        }

        if (timePeriodRepository.count() == 0) {
            timePeriodRepository.saveAll(List.of(
                    period(time(0, 0), time(6, 30), 1.0, 1.0, 1.0, 1.0, 1.0),
                    period(time(6, 30), time(8, 0), 1.5, 1.4, 1.3, 1.2, 1.1),
                    period(time(8, 0), time(10, 30), 2.5, 2.3, 2.2, 1.3, 1.1),
                    period(time(10, 30), time(16, 30), 1.6, 1.6, 1.7, 1.4, 1.2),
                    period(time(16, 30), time(20, 0), 2.6, 2.5, 3.0, 1.6, 1.3),
                    period(time(20, 0), time(23, 0), 1.5, 1.4, 1.7, 1.3, 1.2),
                    period(time(23, 0), time(23, 59), 1.0, 1.0, 1.0, 1.0, 1.0)
            ));
        }

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
            Region region = regionRepository.save(mapper.toRegion(REGION_NAMES.get(dir), officeEngineer(dataset, dir)));
            Map<String, Region> districts = districts(dataset, region);

            List<Region> regionDistricts = new ArrayList<>(districts.values());
            regionDistricts.add(region);

            for (EngineerDto engineer : dataset.engineers()) {
                Region crewRegion = engineer.district() == null ? region : districts.get(engineer.district());
                crews.add(mapper.toCrew(engineer, crewRegion, vehicles, skills));
            }

            for (RequestDto request : dataset.requests()) {
                Region requestRegion = regionDomainService.nearest(regionDistricts, request.location().latitude(), request.location().longitude());
                requests.add(mapper.toRequest(
                        request, requestRegion, jobTypes, vehicles, equipment, statuses.get(RequestStatus.Keyword.IDLE), createdAt));
            }
        }
        engineerCrewRepository.saveAll(crews);
        requestRepository.saveAll(requests);
        requestActionAuditRepository.saveAll(requests.stream()
                .map(RequestActionAudit::requestCreated)
                .toList());

        log.info("Демо-датасет загружен: регионов {}, бригад {}, заявок {}",
                REGION_DIRS.size(), crews.size(), requests.size());
    }

    private static TimePeriod period(LocalTime start, LocalTime end, double monday, double tuesdayThursday, double friday, double saturday, double sunday) {
        TimePeriod period = new TimePeriod(start, end);
        double[] byDay = {monday, tuesdayThursday, tuesdayThursday, tuesdayThursday, friday, saturday, sunday};

        for (int i = 0; i < byDay.length; i++) {
            period.addCoefficient(i + 1, BigDecimal.valueOf(byDay[i]));
        }

        return period;
    }

    private static LocalTime time(int hour, int minute) {
        return LocalTime.of(hour, minute);
    }

    private static EngineerDto officeEngineer(DatasetDto dataset, String dir) {
        return dataset.engineers().stream()
                .filter(engineer -> engineer.district() == null)
                .findFirst()
                .orElseThrow(() -> new InvalidDatasetException("%s: нет инженеров без подрайона, офис региона не определить".formatted(dir)));
    }

    private Map<String, Region> districts(DatasetDto dataset, Region region) {
        Map<String, Region> districts = new LinkedHashMap<>();

        for (EngineerDto engineer : dataset.engineers()) {
            if (engineer.district() == null || districts.containsKey(engineer.district())) {
                continue;
            }

            districts.put(engineer.district(), regionRepository.save(mapper.toDistrict(engineer, region)));
        }

        return districts;
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
                List.of(skills.get(Skill.Keyword.LOCAL_WORK)), 20, 30, 50, JobType.Keyword.LOCAL_WORK, 7));
        jobTypes.put(JobType.Keyword.INSTALLATION, new JobType("Подключение",
                List.of(skills.get(Skill.Keyword.INSTALLATION_OR_ADDON)), 20, 60, 90, JobType.Keyword.INSTALLATION, 3));
        jobTypes.put(JobType.Keyword.ADDON, new JobType("Дозаказ оборудования",
                List.of(skills.get(Skill.Keyword.INSTALLATION_OR_ADDON)), 20, 10, 40, JobType.Keyword.ADDON, 7));
        jobTypes.put(JobType.Keyword.EMERGENCY, new JobType("Глобальная проблема",
                List.of(skills.get(Skill.Keyword.EMERGENCY)), 20, 80, 100, JobType.Keyword.EMERGENCY, 1));
        jobTypeRepository.saveAll(jobTypes.values());
        return jobTypes;
    }

    private Map<Vehicle.Keyword, Vehicle> vehicles() {
        Map<Vehicle.Keyword, Vehicle> vehicles = new EnumMap<>(Vehicle.Keyword.class);
        vehicles.put(Vehicle.Keyword.PEDESTRIAN, new Vehicle("Пешком", Vehicle.Keyword.PEDESTRIAN,
                PEDESTRIAN_SPEED_KMH, PEDESTRIAN_EQUIPMENT_CAPACITY));
        vehicles.put(Vehicle.Keyword.BICYCLE, new Vehicle("Велосипед", Vehicle.Keyword.BICYCLE,
                BICYCLE_SPEED_KMH, BICYCLE_EQUIPMENT_CAPACITY));
        vehicles.put(Vehicle.Keyword.PUBLIC_TRANSPORT, new Vehicle("Общественный транспорт", Vehicle.Keyword.PUBLIC_TRANSPORT,
                PUBLIC_TRANSPORT_SPEED_KMH, PUBLIC_TRANSPORT_EQUIPMENT_CAPACITY));
        vehicles.put(Vehicle.Keyword.CAR, new Vehicle("Автомобиль", Vehicle.Keyword.CAR, CAR_SPEED_KMH, CAR_EQUIPMENT_CAPACITY));
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
