package stb.core.infrastructure.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import stb.core.infrastructure.csv.dto.DatasetDto;
import stb.core.infrastructure.csv.dto.EngineerDto;
import stb.core.infrastructure.csv.dto.GeoPointDto;
import stb.core.infrastructure.csv.dto.ReplanEventDto;
import stb.core.infrastructure.csv.dto.RequestDto;
import stb.core.domain.model.JobType;
import stb.core.domain.model.Vehicle;

class CsvDatasetParserTest {

    private static final String REQUEST = "7;Москва;55.7;37.76;45;9:00;11:00;EMERGENCY;;EMERGENCY";
    private static final String ENGINEER = "1;Бригада;Офис;55.7;37.7;09:00;18:00;EMERGENCY|LOCAL_WORK;BICYCLE";

    private final CsvDatasetParser parser = new CsvDatasetParser();

    @TempDir
    private Path dir;

    @ParameterizedTest
    @ValueSource(strings = {"vostok", "yugo-vostok", "yugo-centr"})
    void parsesDemoDataset(String name) throws URISyntaxException {
        DatasetDto dataset = parser.parse(Path.of(Objects.requireNonNull(getClass().getResource("/demo/dataset/" + name)).toURI()));

        assertThat(dataset.engineers()).hasSizeBetween(10, 15);
        assertThat(dataset.requests()).hasSizeLessThan(100);
        assertThat(dataset.requests()).extracting(RequestDto::requiredSkill).contains(CsvSkill.values());
        assertThat(dataset.requests()).extracting(RequestDto::jobType).contains(JobType.Keyword.values());
        assertThat(dataset.requests()).extracting(RequestDto::requiredVehicle).contains(Vehicle.Keyword.CAR);
        assertThat(dataset.engineers()).extracting(EngineerDto::vehicle).contains(Vehicle.Keyword.values());
        assertThat(dataset.events()).extracting(Object::getClass).containsExactlyInAnyOrder(
                ReplanEventDto.UrgentRequest.class, ReplanEventDto.CancelRequest.class, ReplanEventDto.EngineerUnavailable.class);
    }

    @Test
    void parsesRows() throws IOException {
        write(REQUEST, ENGINEER,
                "CANCEL_REQUEST;11:00;7;;;;;;;;;;",
                "URGENT_REQUEST;14:00;8;;Москва;55.7;37.7;90;14:00;16:00;EMERGENCY;CAR;EMERGENCY");

        DatasetDto dataset = parser.parse(dir);

        assertThat(dataset.requests()).containsExactly(new RequestDto(7, "Москва", new GeoPointDto(55.7, 37.76), 45,
                LocalTime.of(9, 0), LocalTime.of(11, 0), CsvSkill.EMERGENCY, null, JobType.Keyword.EMERGENCY));
        assertThat(dataset.engineers().getFirst().skills()).containsExactlyInAnyOrder(CsvSkill.LOCAL_WORK, CsvSkill.EMERGENCY);
        assertThat(dataset.events().getFirst()).isEqualTo(new ReplanEventDto.CancelRequest(LocalTime.of(11, 0), 7));
        assertThat(dataset.events().getLast()).isInstanceOfSatisfying(ReplanEventDto.UrgentRequest.class, urgent -> {
            assertThat(urgent.request().id()).isEqualTo(8);
            assertThat(urgent.request().requiredVehicle()).isEqualTo(Vehicle.Keyword.CAR);
        });
    }

    @Test
    void rejectsInvalidTime() throws IOException {
        write("1;Москва;55.7;37.7;45;25:00;11:00;LOCAL_WORK;;LOCAL_WORK", ENGINEER);

        assertThatThrownBy(() -> parser.parse(dir))
                .isInstanceOf(InvalidDatasetException.class)
                .hasMessageContaining(RequestColumn.WINDOW_START.header());
    }

    @Test
    void rejectsUnknownSkill() throws IOException {
        write("1;Москва;55.7;37.7;45;9:00;11:00;PLUMBING;;LOCAL_WORK", ENGINEER);

        assertThatThrownBy(() -> parser.parse(dir))
                .isInstanceOf(InvalidDatasetException.class)
                .hasMessageContaining(RequestColumn.REQUIRED_SKILL.header());
    }

    @Test
    void rejectsWindowEndBeforeStart() throws IOException {
        write("1;Москва;55.7;37.7;45;11:00;9:00;LOCAL_WORK;;LOCAL_WORK", ENGINEER);

        assertThatThrownBy(() -> parser.parse(dir))
                .isInstanceOf(InvalidDatasetException.class)
                .hasMessageContaining("заявка 1");
    }

    @Test
    void rejectsSkillNotMatchingJobType() throws IOException {
        write("1;Москва;55.7;37.7;45;9:00;11:00;LOCAL_WORK;;ADDON", ENGINEER);

        assertThatThrownBy(() -> parser.parse(dir))
                .isInstanceOf(InvalidDatasetException.class)
                .hasMessageContaining("не соответствует типу работ");
    }

    @Test
    void rejectsMissingColumns() throws IOException {
        Files.writeString(dir.resolve(DatasetFile.REQUESTS.fileName()), "id;name\n1;Москва\n");

        assertThatThrownBy(() -> parser.parse(dir))
                .isInstanceOf(InvalidDatasetException.class)
                .hasMessageContaining("нет колонок");
    }

    private void write(String request, String engineer, String... events) throws IOException {
        writeCsv(DatasetFile.REQUESTS, List.of(request));
        writeCsv(DatasetFile.ENGINEERS, List.of(engineer));
        writeCsv(DatasetFile.EVENTS, List.of(events));
    }

    private void writeCsv(DatasetFile file, List<String> rows) throws IOException {
        Files.writeString(dir.resolve(file.fileName()),
                String.join(";", file.header()) + "\n" + String.join("\n", rows) + "\n");
    }
}
