package stb.core.api.rest.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import stb.core.application.dev.DevClearTablesUseCase;
import stb.core.infrastructure.csv.DatasetLoader;

@RestController
@RequestMapping("/api/v1/dev")
@RequiredArgsConstructor
@Slf4j
public class DevController {

    private final DevClearTablesUseCase devClearTablesUseCase;
    private final DatasetLoader datasetLoader;

    @PostMapping("/reset")
    public ResponseEntity<?> reloadFromCsv() {
        log.info("Удаление данных в таблицах");
        devClearTablesUseCase.execute();
        log.info("Удаление данных завершено");

        log.info("Загрузка данных из CSV");
        datasetLoader.loadDataFromCsv();
        log.info("Данные из CSV успешно загружены");

        return ResponseEntity.noContent().build();
    }
}
