package stb.core.application.dev;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import stb.core.domain.repository.DevStateResetRepository;

@Component
@RequiredArgsConstructor
public class DevStateResetUseCase {

    private final DevStateResetRepository devStateResetRepository;

    @Transactional
    public void execute() {
        devStateResetRepository.reset();
    }
}
