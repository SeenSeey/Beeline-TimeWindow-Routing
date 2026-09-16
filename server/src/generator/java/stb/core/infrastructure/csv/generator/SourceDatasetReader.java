package stb.core.infrastructure.csv.generator;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Датасет организаторов: на регион пара файлов «Синтетические данные» (заявки и строка «Адрес офиса»)
 * и «Контрольное распределение» (те же заявки в том же порядке, со статусом и бригадой).
 */
final class SourceDatasetReader {

    record SourceRow(
            int id,
            String bkType,
            String hdType,
            LocalTime windowStart,
            LocalTime windowEnd,
            String district,
            String address,
            String status,
            String crew) {
    }

    record SourceRegion(String name, String officeAddress, List<SourceRow> rows) {
    }

    private enum SourceColumn {
        REQUEST_ID("Заявка"),
        BK_TYPE("Тип заявки BK"),
        HD_TYPE("Тип заявки HD"),
        WINDOW_START("Начало"),
        WINDOW_END("Окончание"),
        DISTRICT("Район"),
        ADDRESS("Адрес"),
        STATUS("Статус BK"),
        CREW("Бригада");

        private final String header;

        SourceColumn(String header) {
            this.header = header;
        }
    }

    private static final String SYNTHETIC_SUFFIX = " Синтетические данные.csv";
    private static final String CONTROL_INFIX = " Контрольное распределение";
    private static final String OFFICE_MARKER = "Адрес офиса";
    private static final char BOM = 0xFEFF;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy H:mm");
    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .get();

    private SourceDatasetReader() {
    }

    static List<SourceRegion> read(Path dir) throws IOException {
        List<SourceRegion> regions = new ArrayList<>();
        for (Path file : list(dir)) {
            String fileName = file.getFileName().toString();
            if (fileName.endsWith(SYNTHETIC_SUFFIX)) {
                String region = fileName.substring(0, fileName.length() - SYNTHETIC_SUFFIX.length());
                regions.add(readRegion(region, file, controlFile(dir, region)));
            }
        }
        return regions;
    }

    private static SourceRegion readRegion(String region, Path syntheticFile, Path controlFile) throws IOException {
        String officeAddress = null;
        List<CSVRecord> requests = new ArrayList<>();
        for (CSVRecord record : readRecords(syntheticFile)) {
            String first = record.get(SourceColumn.REQUEST_ID.header);
            if (first.equalsIgnoreCase(OFFICE_MARKER)) {
                // в строке «Адрес офиса» сам адрес лежит в колонке типа заявки
                officeAddress = cleanAddress(record.get(SourceColumn.BK_TYPE.header));
            } else if (!first.isEmpty()) {
                requests.add(record);
            }
        }
        List<CSVRecord> control = readRecords(controlFile).stream()
                .filter(record -> !record.get(SourceColumn.REQUEST_ID.header).isEmpty())
                .toList();
        if (officeAddress == null || requests.size() != control.size()) {
            throw new IllegalStateException("Регион %s: не найден адрес офиса или число заявок в файлах не совпадает".formatted(region));
        }

        List<SourceRow> rows = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            CSVRecord request = requests.get(i);
            CSVRecord controlRecord = control.get(i);
            String address = cleanAddress(request.get(SourceColumn.ADDRESS.header));
            if (!cleanAddress(controlRecord.get(SourceColumn.ADDRESS.header)).startsWith(address)) {
                throw new IllegalStateException("Регион %s, заявка %s: в контрольном распределении другой адрес"
                        .formatted(region, request.get(SourceColumn.REQUEST_ID.header)));
            }
            rows.add(new SourceRow(
                    Integer.parseInt(request.get(SourceColumn.REQUEST_ID.header)),
                    request.get(SourceColumn.BK_TYPE.header),
                    request.get(SourceColumn.HD_TYPE.header),
                    LocalDateTime.parse(request.get(SourceColumn.WINDOW_START.header), DATE_TIME).toLocalTime(),
                    LocalDateTime.parse(request.get(SourceColumn.WINDOW_END.header), DATE_TIME).toLocalTime(),
                    request.get(SourceColumn.DISTRICT.header),
                    address,
                    controlRecord.get(SourceColumn.STATUS.header),
                    controlRecord.get(SourceColumn.CREW.header).isEmpty() ? null : controlRecord.get(SourceColumn.CREW.header)));
        }
        return new SourceRegion(region, officeAddress, List.copyOf(rows));
    }

    private static Path controlFile(Path dir, String region) throws IOException {
        return list(dir).stream()
                .filter(file -> file.getFileName().toString().startsWith(region + CONTROL_INFIX))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Нет контрольного распределения для региона " + region));
    }

    private static List<Path> list(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.sorted().toList();
        }
    }

    private static List<CSVRecord> readRecords(Path file) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        if (!content.isEmpty() && content.charAt(0) == BOM) {
            content = content.substring(1);
        }
        try (CSVParser parser = FORMAT.parse(new StringReader(content))) {
            return parser.getRecords();
        }
    }

    private static String cleanAddress(String address) {
        return address.replace("г.Город Москва", "Город Москва");
    }
}
