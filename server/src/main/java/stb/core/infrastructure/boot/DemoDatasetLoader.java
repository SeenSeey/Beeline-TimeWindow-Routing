package stb.core.infrastructure.boot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import stb.core.infrastructure.csv.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemoDatasetLoader implements CommandLineRunner {

    private final DatasetLoader datasetLoader;

    @Override
    @Transactional
    public void run(String... args) {
        datasetLoader.loadDataFromCsv();
    }
}
