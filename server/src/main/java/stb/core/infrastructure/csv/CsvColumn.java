package stb.core.infrastructure.csv;

import java.util.Locale;

public interface CsvColumn {

    String name();

    default String header() {
        return name().toLowerCase(Locale.ROOT);
    }
}
