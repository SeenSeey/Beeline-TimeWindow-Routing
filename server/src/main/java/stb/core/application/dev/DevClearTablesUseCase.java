package stb.core.application.dev;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.repository.DevStateResetRepository;

@Component
@RequiredArgsConstructor
public class DevClearTablesUseCase {

    private final DevStateResetRepository devStateResetRepository;

    @Transactional
    public void execute() {
        devStateResetRepository.clearAll();
    }
}
