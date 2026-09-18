package stb.core.infrastructure.csv;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.io.input.BOMInputStream;

import stb.core.infrastructure.csv.dto.DatasetDto;
import stb.core.infrastructure.csv.dto.EngineerDto;
import stb.core.infrastructure.csv.dto.GeoPointDto;
import stb.core.infrastructure.csv.dto.ReplanEventDto;
import stb.core.infrastructure.csv.dto.RequestDto;
import stb.core.domain.model.JobType;
import stb.core.domain.model.Vehicle;

public class CsvDatasetParser {

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .get();

    private final DatasetValidator validator = new DatasetValidator();

    public DatasetDto parse(Path dir) {
        Path eventsFile = dir.resolve(DatasetFile.EVENTS.fileName());
        DatasetDto dataset = new DatasetDto(
                read(dir, DatasetFile.REQUESTS, CsvDatasetParser::toRequest),
                read(dir, DatasetFile.ENGINEERS, CsvDatasetParser::toEngineer),
                Files.exists(eventsFile) ? read(dir, DatasetFile.EVENTS, CsvDatasetParser::toEvent) : List.of());
        validator.validate(dataset);
        return dataset;
    }

    private static <T> List<T> read(Path dir, DatasetFile file, Function<CsvRow, T> mapper) {
        Path path = dir.resolve(file.fileName());
        try (Reader reader = new InputStreamReader(BOMInputStream.builder().setPath(path).get(), StandardCharsets.UTF_8);
                CSVParser parser = FORMAT.parse(reader)) {
            List<String> missing = file.header().stream().filter(column -> !parser.getHeaderNames().contains(column)).toList();
            if (!missing.isEmpty()) {
                throw new InvalidDatasetException(file.fileName() + ": нет колонок " + missing);
            }
            return parser.stream().map(record -> mapper.apply(new CsvRow(file.fileName(), record))).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static RequestDto toRequest(CsvRow row) {
        return new RequestDto(
                row.intValue(RequestColumn.ID),
                row.string(RequestColumn.ADDRESS),
                new GeoPointDto(row.doubleValue(RequestColumn.LATITUDE), row.doubleValue(RequestColumn.LONGITUDE)),
                row.intValue(RequestColumn.DURATION_MINUTES),
                row.time(RequestColumn.WINDOW_START),
                row.time(RequestColumn.WINDOW_END),
                row.enumValue(RequestColumn.REQUIRED_SKILL, CsvSkill.class),
                row.optionalEnum(RequestColumn.REQUIRED_VEHICLE, Vehicle.Keyword.class),
                row.enumValue(RequestColumn.JOB_TYPE, JobType.Keyword.class));
    }

    private static EngineerDto toEngineer(CsvRow row) {
        return new EngineerDto(
                row.intValue(EngineerColumn.ID),
                row.string(EngineerColumn.NAME),
                row.string(EngineerColumn.START_ADDRESS),
                new GeoPointDto(row.doubleValue(EngineerColumn.START_LATITUDE), row.doubleValue(EngineerColumn.START_LONGITUDE)),
                row.time(EngineerColumn.SHIFT_START),
                row.time(EngineerColumn.SHIFT_END),
                Set.copyOf(row.enumList(EngineerColumn.SKILLS, CsvSkill.class)),
                row.enumValue(EngineerColumn.VEHICLE, Vehicle.Keyword.class));
    }

    private static ReplanEventDto toEvent(CsvRow row) {
        LocalTime eventTime = row.time(EventColumn.EVENT_TIME);
        return switch (row.enumValue(EventColumn.EVENT_TYPE, ReplanEventType.class)) {
            case URGENT_REQUEST -> new ReplanEventDto.UrgentRequest(eventTime, toRequest(row.withPrefix(EventColumn.REQUEST_PREFIX)));
            case CANCEL_REQUEST -> new ReplanEventDto.CancelRequest(eventTime, row.intValue(EventColumn.REQUEST_ID));
            case ENGINEER_UNAVAILABLE -> new ReplanEventDto.EngineerUnavailable(eventTime, row.intValue(EventColumn.ENGINEER_ID));
        };
    }
}
