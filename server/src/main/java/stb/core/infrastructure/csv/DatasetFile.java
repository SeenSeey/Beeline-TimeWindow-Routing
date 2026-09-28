package stb.core.infrastructure.csv;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public enum DatasetFile {
    REQUESTS(headers(RequestColumn.values())),
    ENGINEERS(headers(EngineerColumn.values())),
    EVENTS(Stream.concat(
            headers(EventColumn.values()).stream(),
            Arrays.stream(RequestColumn.values())
                    .filter(column -> column != RequestColumn.ID)
                    .map(column -> EventColumn.REQUEST_PREFIX + column.header()))
            .toList());

    private final List<String> header;

    DatasetFile(List<String> header) {
        this.header = header;
    }

    public String fileName() {
        return name().toLowerCase(Locale.ROOT) + ".csv";
    }

    public List<String> header() {
        return header;
    }

    private static List<String> headers(CsvColumn[] columns) {
        return Arrays.stream(columns).filter(column -> !column.optional()).map(CsvColumn::header).toList();
    }
}
