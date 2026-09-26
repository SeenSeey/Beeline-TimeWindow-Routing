package stb.core.infrastructure.csv.generator;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import stb.core.infrastructure.csv.CsvColumn;
import stb.core.infrastructure.csv.CsvEquipment;
import stb.core.infrastructure.csv.CsvSkill;
import stb.core.infrastructure.csv.DatasetFile;
import stb.core.infrastructure.csv.EngineerColumn;
import stb.core.infrastructure.csv.EventColumn;
import stb.core.infrastructure.csv.ReplanEventType;
import stb.core.infrastructure.csv.RequestColumn;
import stb.core.infrastructure.csv.dto.DatasetDto;
import stb.core.infrastructure.csv.dto.EngineerDto;
import stb.core.infrastructure.csv.dto.GeoPointDto;
import stb.core.infrastructure.csv.dto.ReplanEventDto;
import stb.core.infrastructure.csv.dto.RequestDto;
import stb.core.infrastructure.csv.generator.SourceDatasetReader.SourceRegion;
import stb.core.infrastructure.csv.generator.SourceDatasetReader.SourceRow;
import stb.core.domain.model.JobType;
import stb.core.domain.model.Vehicle;

public final class DatasetGenerator {

    private static final Logger log = LoggerFactory.getLogger(DatasetGenerator.class);

    private static final Map<String, String> REGION_DIRS = Map.of(
            "Восток", "vostok",
            "Юго-восток", "yugo-vostok",
            "Югоцентр", "yugo-centr");

    private static final Map<String, JobType.Keyword> JOB_TYPE_BY_BK_TYPE = Map.of(
            "Подключение", JobType.Keyword.INSTALLATION,
            "Дозаказ", JobType.Keyword.ADDON,
            "Локальная заявка", JobType.Keyword.LOCAL_WORK,
            "Глобальная проблема", JobType.Keyword.EMERGENCY);

    private static final Map<JobType.Keyword, Integer> ON_SITE_MINUTES = Map.of(
            JobType.Keyword.INSTALLATION, 70,
            JobType.Keyword.ADDON, 20,
            JobType.Keyword.LOCAL_WORK, 30,
            JobType.Keyword.EMERGENCY, 80);

    private static final Set<String> CAR_HD_TYPES = Set.of(
            "Работа с кабелем",
            "Авария");

    private static final Set<String> FAR_DISTRICTS = Set.of("Кашира", "Ступино", "Домодедово");

    private record Shift(LocalTime start, LocalTime end) {
    }

    private static final List<Shift> SHIFTS = List.of(
            new Shift(LocalTime.of(9, 0), LocalTime.of(22, 0)),
            new Shift(LocalTime.of(9, 0), LocalTime.of(18, 0)),
            new Shift(LocalTime.of(10, 0), LocalTime.of(19, 0)),
            new Shift(LocalTime.of(13, 0), LocalTime.of(22, 0)));

    private static final double EARTH_RADIUS_KM = 6371;
    private static final double CAR_HOP_KM = 8;
    private static final double CAR_RADIUS_KM = 15;
    private static final double PUBLIC_TRANSPORT_HOP_KM = 4;
    private static final double BICYCLE_HOP_KM = 2;

    private static final String CANCELED_STATUS = "Отменена";
    private static final LocalTime CANCEL_TIME = LocalTime.of(11, 0);
    private static final LocalTime UNAVAILABLE_TIME = LocalTime.of(13, 0);
    private static final LocalTime URGENT_TIME = LocalTime.of(14, 0);
    private static final int URGENT_WINDOW_HOURS = 2;

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final CSVFormat CSV = CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setRecordSeparator("\n")
            .get();

    private enum CoordinatesColumn implements CsvColumn {
        ADDRESS,
        LATITUDE,
        LONGITUDE
    }

    private record Crew(String name, Set<CsvSkill> skills, int requestCount, Vehicle.Keyword vehicle) {
    }

    private DatasetGenerator() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException("Аргументы: <папка датасета> <coordinates.csv> <папка результата>");
        }
        Path sourceDir = Path.of(args[0]);
        Map<String, GeoPointDto> coordinates = readCoordinates(Path.of(args[1]));
        Path outputDir = Path.of(args[2]);

        int engineerIdOffset = 0;
        for (SourceRegion region : SourceDatasetReader.read(sourceDir)) {
            Path dir = outputDir.resolve(REGION_DIRS.get(region.name()));
            DatasetDto dataset = generate(region, coordinates, engineerIdOffset);
            engineerIdOffset += dataset.engineers().size();
            write(dataset, dir);
            log.info("{}: заявок {}, инженеров {}, событий {} -> {}", region.name(),
                    dataset.requests().size(), dataset.engineers().size(), dataset.events().size(), dir);
        }
    }

    private static DatasetDto generate(SourceRegion region, Map<String, GeoPointDto> coordinates, int engineerIdOffset) {
        List<RequestDto> requests = region.rows().stream()
                .map(row -> request(row, coordinates))
                .toList();
        GeoPointDto office = locate(coordinates, region.officeAddress());
        List<Crew> crews = crews(region, coordinates, office);

        List<EngineerDto> engineers = new ArrayList<>();
        for (int i = 0; i < crews.size(); i++) {
            Crew crew = crews.get(i);
            Shift shift = SHIFTS.get(i % SHIFTS.size());
            engineers.add(new EngineerDto(engineerIdOffset + i + 1, crew.name(), region.officeAddress(), office,
                    shift.start(), shift.end(), crew.skills(), crew.vehicle()));
        }
        return new DatasetDto(requests, List.copyOf(engineers), events(region, requests, List.copyOf(engineers), crews));
    }

    private static RequestDto request(SourceRow row, Map<String, GeoPointDto> coordinates) {
        JobType.Keyword jobType = jobType(row.bkType());
        return new RequestDto(
                row.id(),
                row.address(),
                locate(coordinates, row.address()),
                ON_SITE_MINUTES.get(jobType),
                row.windowStart(),
                row.windowEnd(),
                needsCar(row) ? Vehicle.Keyword.CAR : null,
                jobType,
                equipment(jobType, row.id()));
    }

    private static boolean needsCar(SourceRow row) {
        return CAR_HD_TYPES.contains(row.hdType()) || FAR_DISTRICTS.contains(row.district());
    }

    private static CsvEquipment equipment(JobType.Keyword jobType, int requestId) {
        return switch (jobType) {
            case INSTALLATION -> CsvEquipment.ROUTER;
            case ADDON -> requestId % 2 == 0 ? CsvEquipment.SMART_SPEAKER : CsvEquipment.SET_TOP_BOX;
            case LOCAL_WORK, EMERGENCY -> null;
        };
    }

    private static List<Crew> crews(SourceRegion region, Map<String, GeoPointDto> coordinates, GeoPointDto office) {
        Map<String, List<SourceRow>> rowsByCrew = region.rows().stream()
                .filter(row -> row.crew() != null)
                .collect(Collectors.groupingBy(SourceRow::crew, LinkedHashMap::new, Collectors.toList()));
        return rowsByCrew.entrySet().stream()
                .map(entry -> crew(entry.getKey(), entry.getValue(), coordinates, office))
                .sorted(comparingInt((Crew crew) -> crew.skills().size())
                        .thenComparingInt(Crew::requestCount)
                        .reversed())
                .toList();
    }

    private static Crew crew(String name, List<SourceRow> rows, Map<String, GeoPointDto> coordinates, GeoPointDto office) {
        EnumSet<CsvSkill> skills = EnumSet.noneOf(CsvSkill.class);
        rows.forEach(row -> skills.add(CsvSkill.of(jobType(row.bkType()))));
        return new Crew(name, Set.copyOf(skills), rows.size(), vehicle(rows, coordinates, office));
    }

    private static Vehicle.Keyword vehicle(List<SourceRow> rows, Map<String, GeoPointDto> coordinates, GeoPointDto office) {
        if (rows.stream().anyMatch(DatasetGenerator::needsCar)) {
            return Vehicle.Keyword.CAR;
        }
        List<GeoPointDto> route = rows.stream()
                .sorted(comparing(SourceRow::windowStart).thenComparing(SourceRow::windowEnd))
                .map(row -> locate(coordinates, row.address()))
                .toList();
        double maxHop = 0;
        for (int i = 1; i < route.size(); i++) {
            maxHop = Math.max(maxHop, distanceKm(route.get(i - 1), route.get(i)));
        }
        double radius = route.stream().mapToDouble(point -> distanceKm(office, point)).max().orElse(0);

        if (maxHop > CAR_HOP_KM || radius > CAR_RADIUS_KM) {
            return Vehicle.Keyword.CAR;
        }
        if (maxHop > PUBLIC_TRANSPORT_HOP_KM) {
            return Vehicle.Keyword.PUBLIC_TRANSPORT;
        }
        if (maxHop > BICYCLE_HOP_KM) {
            return Vehicle.Keyword.BICYCLE;
        }
        return Vehicle.Keyword.PEDESTRIAN;
    }

    private static double distanceKm(GeoPointDto from, GeoPointDto to) {
        double fromLatitude = Math.toRadians(from.latitude());
        double toLatitude = Math.toRadians(to.latitude());
        double deltaLatitude = toLatitude - fromLatitude;
        double deltaLongitude = Math.toRadians(to.longitude() - from.longitude());
        double haversine = Math.pow(Math.sin(deltaLatitude / 2), 2)
                + Math.cos(fromLatitude) * Math.cos(toLatitude) * Math.pow(Math.sin(deltaLongitude / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(haversine));
    }

    private static List<ReplanEventDto> events(SourceRegion region, List<RequestDto> requests,
            List<EngineerDto> engineers, List<Crew> crews) {
        List<SourceRow> rows = region.rows();
        RequestDto canceled = null;
        for (int i = 0; i < rows.size() && canceled == null; i++) {
            if (CANCELED_STATUS.equals(rows.get(i).status()) && !rows.get(i).windowStart().isBefore(LocalTime.NOON)) {
                canceled = requests.get(i);
            }
        }
        if (canceled == null) {
            throw new IllegalStateException(region.name() + ": нет отменённой заявки с окном после полудня");
        }

        int unavailable = 1;
        for (int i = 2; i < crews.size(); i++) {
            if (crews.get(i).requestCount() > crews.get(unavailable).requestCount()) {
                unavailable = i;
            }
        }

        RequestDto emergency = requests.reversed().stream()
                .filter(request -> request.jobType() == JobType.Keyword.EMERGENCY)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(region.name() + ": нет аварийных заявок"));
        int urgentId = requests.stream().mapToInt(RequestDto::id).max().orElseThrow() + 1;
        RequestDto urgent = new RequestDto(urgentId, emergency.address(), emergency.location(),
                ON_SITE_MINUTES.get(JobType.Keyword.EMERGENCY), URGENT_TIME, URGENT_TIME.plusHours(URGENT_WINDOW_HOURS),
                null, JobType.Keyword.EMERGENCY, null);

        return List.of(
                new ReplanEventDto.CancelRequest(CANCEL_TIME, canceled.id()),
                new ReplanEventDto.EngineerUnavailable(UNAVAILABLE_TIME, engineers.get(unavailable).id()),
                new ReplanEventDto.UrgentRequest(URGENT_TIME, urgent));
    }

    private static JobType.Keyword jobType(String bkType) {
        JobType.Keyword jobType = JOB_TYPE_BY_BK_TYPE.get(bkType);
        if (jobType == null) {
            throw new IllegalStateException("Неизвестный тип заявки BK: " + bkType);
        }
        return jobType;
    }

    private static GeoPointDto locate(Map<String, GeoPointDto> coordinates, String address) {
        GeoPointDto point = coordinates.get(address);
        if (point == null) {
            throw new IllegalStateException("Нет координат в coordinates.csv для адреса: " + address);
        }
        return point;
    }

    private static Map<String, GeoPointDto> readCoordinates(Path file) throws IOException {
        Map<String, GeoPointDto> coordinates = new HashMap<>();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
                CSVParser parser = CSV.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader)) {
            for (CSVRecord record : parser) {
                coordinates.put(record.get(CoordinatesColumn.ADDRESS.header()), new GeoPointDto(
                        Double.parseDouble(record.get(CoordinatesColumn.LATITUDE.header())),
                        Double.parseDouble(record.get(CoordinatesColumn.LONGITUDE.header()))));
            }
        }
        return coordinates;
    }

    private static void write(DatasetDto dataset, Path dir) throws IOException {
        Files.createDirectories(dir);
        writeCsv(dir, DatasetFile.REQUESTS, dataset.requests().stream().map(request -> requestValues(request, "")).toList());
        writeCsv(dir, DatasetFile.ENGINEERS, dataset.engineers().stream().map(DatasetGenerator::engineerRow).toList());
        writeCsv(dir, DatasetFile.EVENTS, dataset.events().stream().map(DatasetGenerator::eventRow).toList());
    }

    private static void writeCsv(Path dir, DatasetFile file, List<Map<String, Object>> rows) throws IOException {
        try (Writer writer = Files.newBufferedWriter(dir.resolve(file.fileName()), StandardCharsets.UTF_8);
                CSVPrinter printer = CSV.builder().setHeader(file.header().toArray(String[]::new)).get().print(writer)) {
            for (Map<String, Object> row : rows) {
                printer.printRecord(file.header().stream().map(row::get).toList());
            }
        }
    }

    private static Map<String, Object> requestValues(RequestDto request, String prefix) {
        Map<String, Object> row = new HashMap<>();
        row.put(prefix + RequestColumn.ID.header(), request.id());
        row.put(prefix + RequestColumn.ADDRESS.header(), request.address());
        row.put(prefix + RequestColumn.LATITUDE.header(), coordinate(request.location().latitude()));
        row.put(prefix + RequestColumn.LONGITUDE.header(), coordinate(request.location().longitude()));
        row.put(prefix + RequestColumn.DURATION_MINUTES.header(), request.durationMinutes());
        row.put(prefix + RequestColumn.WINDOW_START.header(), request.windowStart().format(TIME));
        row.put(prefix + RequestColumn.WINDOW_END.header(), request.windowEnd().format(TIME));
        row.put(prefix + RequestColumn.REQUIRED_VEHICLE.header(), request.requiredVehicle());
        row.put(prefix + RequestColumn.JOB_TYPE.header(), request.jobType());
        row.put(prefix + RequestColumn.REQUIRED_EQUIPMENT.header(), request.requiredEquipment());
        return row;
    }

    private static Map<String, Object> engineerRow(EngineerDto engineer) {
        Map<String, Object> row = new HashMap<>();
        row.put(EngineerColumn.ID.header(), engineer.id());
        row.put(EngineerColumn.NAME.header(), engineer.name());
        row.put(EngineerColumn.START_ADDRESS.header(), engineer.startAddress());
        row.put(EngineerColumn.START_LATITUDE.header(), coordinate(engineer.start().latitude()));
        row.put(EngineerColumn.START_LONGITUDE.header(), coordinate(engineer.start().longitude()));
        row.put(EngineerColumn.SHIFT_START.header(), engineer.shiftStart().format(TIME));
        row.put(EngineerColumn.SHIFT_END.header(), engineer.shiftEnd().format(TIME));
        row.put(EngineerColumn.SKILLS.header(),
                engineer.skills().stream().sorted().map(Enum::name).collect(Collectors.joining("|")));
        row.put(EngineerColumn.VEHICLE.header(), engineer.vehicle());
        return row;
    }

    private static Map<String, Object> eventRow(ReplanEventDto event) {
        Map<String, Object> row = new HashMap<>();
        row.put(EventColumn.EVENT_TIME.header(), event.eventTime().format(TIME));
        switch (event) {
            case ReplanEventDto.UrgentRequest urgent -> {
                row.put(EventColumn.EVENT_TYPE.header(), ReplanEventType.URGENT_REQUEST);
                row.putAll(requestValues(urgent.request(), EventColumn.REQUEST_PREFIX));
            }
            case ReplanEventDto.CancelRequest cancel -> {
                row.put(EventColumn.EVENT_TYPE.header(), ReplanEventType.CANCEL_REQUEST);
                row.put(EventColumn.REQUEST_ID.header(), cancel.requestId());
            }
            case ReplanEventDto.EngineerUnavailable unavailable -> {
                row.put(EventColumn.EVENT_TYPE.header(), ReplanEventType.ENGINEER_UNAVAILABLE);
                row.put(EventColumn.ENGINEER_ID.header(), unavailable.engineerId());
            }
        }
        return row;
    }

    private static String coordinate(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }
}
