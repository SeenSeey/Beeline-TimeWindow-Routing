package stb.core.domain.service.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Request;
import stb.core.domain.repository.projection.CrewLatestSlot;
import stb.core.domain.service.scheduler.impl.logic.speed.SpeedProfile;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleContext {

    private List<Request> requests;
    private List<EngineerCrew> engineerCrews;
    private TravelMatrix travelMatrix;
    private SpeedProfile speedProfile;
    private List<CrewLatestSlot> crewsLatestSlots;
    private Map<Integer, Integer> crewsUsedInSchedule;
    private boolean replanning;
}
