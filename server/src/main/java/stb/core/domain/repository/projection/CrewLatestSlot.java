package stb.core.domain.repository.projection;

import stb.core.domain.common.GeopositionProvider;

import java.time.LocalTime;

public interface CrewLatestSlot extends GeopositionProvider {

    int getSlotId();
    int getRequestId();
    LocalTime getStartTimeWindow();
    LocalTime getEndTimeWindow();
    int getCrewId();
    double getLatitude();
    double getLongitude();
}
