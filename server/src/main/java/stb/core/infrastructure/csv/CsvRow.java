package stb.core.infrastructure.csv;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.csv.CSVRecord;


class CsvRow {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H:mm");
    private static final String LIST_SEPARATOR = "\\|";

    private final String file;
    private final CSVRecord record;
    private final String prefix;

    CsvRow(String file, CSVRecord record) {
        this(file, record, "");
    }

    private CsvRow(String file, CSVRecord record, String prefix) {
        this.file = file;
        this.record = record;
        this.prefix = prefix;
    }

    CsvRow withPrefix(String prefix) {
        return new CsvRow(file, record, prefix);
    }

    String string(CsvColumn column) {
        String value = value(column);
        if (value == null) {
            throw error(column, "не заполнено");
        }
        return value;
    }

    private String value(CsvColumn column) {
        String name = header(column);
        return record.isSet(name) && !record.get(name).isEmpty() ? record.get(name) : null;
    }

    int intValue(CsvColumn column) {
        return convert(column, Integer::parseInt, "целое число");
    }

    boolean booleanValue(CsvColumn column) {
        return convert(column, CsvRow::parseBoolean, "true или false");
    }

    double doubleValue(CsvColumn column) {
        return convert(column, Double::parseDouble, "число");
    }

    LocalTime time(CsvColumn column) {
        return convert(column, value -> LocalTime.parse(value, TIME), "время HH:MM");
    }

    <E extends Enum<E>> E enumValue(CsvColumn column, Class<E> type) {
        return convert(column, value -> Enum.valueOf(type, value), "одно из " + Arrays.toString(type.getEnumConstants()));
    }

    <E extends Enum<E>> E optionalEnum(CsvColumn column, Class<E> type) {
        return value(column) == null ? null : enumValue(column, type);
    }

    <E extends Enum<E>> List<E> enumList(CsvColumn column, Class<E> type) {
        return convert(column,
                value -> Arrays.stream(value.split(LIST_SEPARATOR)).map(item -> Enum.valueOf(type, item.trim())).toList(),
                "значения через | из " + Arrays.toString(type.getEnumConstants()));
    }

    private static boolean parseBoolean(String value) {
        return switch (value) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException(value);
        };
    }

    private <T> T convert(CsvColumn column, Function<String, T> converter, String expected) {
        String value = string(column);
        try {
            return converter.apply(value);
        } catch (RuntimeException e) {
            throw error(column, "ожидалось %s, получено '%s'".formatted(expected, value));
        }
    }

    private InvalidDatasetException error(CsvColumn column, String message) {
        return new InvalidDatasetException("%s, запись %d, %s: %s"
                .formatted(file, record.getRecordNumber(), header(column), message));
    }

    private String header(CsvColumn column) {
        return prefix + column.header();
    }
}
