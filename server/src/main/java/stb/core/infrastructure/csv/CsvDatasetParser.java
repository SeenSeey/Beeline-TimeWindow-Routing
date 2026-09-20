package stb.core.infrastructure.csv;

import java.io.IOException;
import java.io.InputStream;
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
        return parse(file -> {
            Path path = dir.resolve(file.fileName());
            try {
                return Files.exists(path) ? Files.newInputStream(path) : null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public DatasetDto parse(Function<DatasetFile, InputStream> source) {
        DatasetDto dataset = new DatasetDto(
                read(source, DatasetFile.REQUESTS, CsvDatasetParser::toRequest),
                read(source, DatasetFile.ENGINEERS, CsvDatasetParser::toEngineer),
                read(source, DatasetFile.EVENTS, CsvDatasetParser::toEvent));
        validator.validate(dataset);
        return dataset;
    }

    private static <T> List<T> read(Function<DatasetFile, InputStream> source, DatasetFile file, Function<CsvRow, T> mapper) {
        InputStream input = source.apply(file);
        if (input == null) {
            if (file != DatasetFile.EVENTS) {
                throw new InvalidDatasetException("нет файла " + file.fileName());
            }
            return List.of();
        }
        try (Reader reader = new InputStreamReader(BOMInputStream.builder().setInputStream(input).get(), StandardCharsets.UTF_8);
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
                row.optionalEnum(RequestColumn.REQUIRED_VEHICLE, Vehicle.Keyword.class),
                row.enumValue(RequestColumn.JOB_TYPE, JobType.Keyword.class),
                row.optionalEnumList(RequestColumn.REQUIRED_EQUIPMENT, CsvEquipment.class));
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
