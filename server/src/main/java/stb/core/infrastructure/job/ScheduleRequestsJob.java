package stb.core.infrastructure.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import stb.core.application.dev.DevClearTablesUseCase;
import stb.core.application.schedule.ScheduleInitialBuildUseCase;
import stb.core.infrastructure.csv.DatasetLoader;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleRequestsJob {

    private final ScheduleInitialBuildUseCase scheduleInitialBuildUseCase;
    private final DevClearTablesUseCase devClearTablesUseCase;
    private final DatasetLoader datasetLoader;

    @Scheduled(cron = "0 1 0 * * *")
    public void execute() {
        LocalDate targetDate = LocalDate.now();

        log.info("Начало расчёта расписания на {}", targetDate);

        log.info("Удаление данных в таблицах");
        devClearTablesUseCase.execute();
        log.info("Удаление данных завершено");

        log.info("Загрузка данных из CSV");
        datasetLoader.loadDataFromCsv();
        log.info("Данные из CSV успешно загружены");

        log.info("Формирование расписания");
        scheduleInitialBuildUseCase.execute();
        log.info("Расписание на {} успешно сформировано", targetDate);
    }

}
