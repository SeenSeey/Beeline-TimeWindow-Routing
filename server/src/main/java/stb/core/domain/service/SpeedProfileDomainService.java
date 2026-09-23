package stb.core.domain.service;

import org.springframework.stereotype.Service;
import stb.core.domain.service.scheduler.impl.logic.speed.SpeedInterval;
import stb.core.domain.service.scheduler.impl.logic.speed.SpeedProfile;

import java.time.LocalTime;
import java.util.List;

@Service
public class SpeedProfileDomainService {

    public SpeedProfile createSpeedProfile() {
        return new SpeedProfile(
                List.of(new SpeedInterval(LocalTime.of(0, 0), LocalTime.of(7, 0), 1.00),
                        new SpeedInterval(LocalTime.of(7, 0), LocalTime.of(9, 0), 0.90),
                        new SpeedInterval(LocalTime.of(9, 0), LocalTime.of(16, 0), 1.10),
                        new SpeedInterval(LocalTime.of(16, 0), LocalTime.of(19, 0), 0.75),
                        new SpeedInterval(LocalTime.of(19, 0), LocalTime.MAX, 1.00)
                )
        );
    }
}
