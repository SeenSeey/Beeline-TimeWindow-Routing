package stb.core.domain.replanning;

import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Request;

import java.util.Objects;

public sealed interface AutomaticReplanContext {

    record HigherPriorityRequest(Request causeRequest) implements AutomaticReplanContext {
        public HigherPriorityRequest {
            Objects.requireNonNull(causeRequest, "causeRequest не должна быть null");
        }
    }

    record RequestCancellation(Request causeRequest) implements AutomaticReplanContext {
        public RequestCancellation {
            Objects.requireNonNull(causeRequest, "causeRequest не должна быть null");
        }
    }

    record CrewOffShift(EngineerCrew causeCrew) implements AutomaticReplanContext {
        public CrewOffShift {
            Objects.requireNonNull(causeCrew, "causeCrew не должна быть null");
        }
    }
}
