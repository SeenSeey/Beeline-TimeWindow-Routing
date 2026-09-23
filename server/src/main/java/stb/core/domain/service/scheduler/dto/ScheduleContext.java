package stb.core.domain.service.scheduler.dto;

import lombok.Data;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Request;
import stb.core.domain.service.scheduler.impl.logic.speed.SpeedProfile;

import java.util.List;

@Data
public class ScheduleContext {

    private List<Request> requests;
    private List<EngineerCrew> engineerCrews;
    private TravelMatrix travelMatrix;
    private SpeedProfile speedProfile;
}
