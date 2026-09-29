package stb.core.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stb.core.domain.repository.TimeCoefficientRepository;
import stb.core.domain.repository.projection.SpeedIntervalProjection;
import stb.core.domain.service.scheduler.impl.logic.speed.SpeedProfile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpeedProfileDomainService {

    private final TimeCoefficientRepository timeCoefficientRepository;
    private final Clock clock;

    public SpeedProfile createSpeedProfile() {
        LocalDateTime dateTime = LocalDateTime.now(clock);
        List<SpeedIntervalProjection> speedIntervals = timeCoefficientRepository.findIntervals(dateTime.getMonthValue(), dateTime.getDayOfWeek().getValue());
        return SpeedProfile.from(speedIntervals);
    }
}
