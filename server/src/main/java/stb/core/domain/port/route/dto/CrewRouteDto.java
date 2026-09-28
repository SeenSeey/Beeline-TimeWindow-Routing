package stb.core.domain.port.route.dto;

import java.util.List;

/**
 * A crew's route over the day, leg by leg: from the region's office to the first visit, then from each
 * visit to the next. Leg {@code i} leads to the crew's {@code i}-th visit.
 *
 * @param distanceKilometers the legs together
 * @param legs               in visit order
 */
public record CrewRouteDto(double distanceKilometers, List<Leg> legs) {

    /**
     * @param index              position in the route, from 0
     * @param fromRequestId      the request the leg starts at; null for the leg from the office
     * @param toRequestId        the request the leg leads to
     * @param distanceKilometers road distance of the leg; 0 between two requests at one address
     * @param polyline           the road geometry, in travel order, as an encoded polyline (precision 5,
     *                           latitude first); empty between two requests at one address
     */
    public record Leg(int index,
                      Integer fromRequestId,
                      int toRequestId,
                      double distanceKilometers,
                      String polyline) {
    }
}
