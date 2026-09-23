package stb.core.domain.service.scheduler.dto;

import lombok.Data;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Request;

import java.util.List;

@Data
public class InputScheduleContext {

    private List<Request> requests;
    private List<EngineerCrew> engineerCrews;
}
