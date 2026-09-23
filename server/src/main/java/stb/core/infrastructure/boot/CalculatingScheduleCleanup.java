package stb.core.infrastructure.boot;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;
import stb.core.application.schedule.ScheduleCalculatingDeleteUseCase;

@Component
@RequiredArgsConstructor
public class CalculatingScheduleCleanup implements SmartInitializingSingleton {

    private final ScheduleCalculatingDeleteUseCase scheduleCalculatingDeleteUseCase;

    @Override
    public void afterSingletonsInstantiated() {
        scheduleCalculatingDeleteUseCase.execute();
    }
}
