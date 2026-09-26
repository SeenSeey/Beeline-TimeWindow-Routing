package stb.core.domain.service;

import org.springframework.stereotype.Service;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Equipment;
import stb.core.domain.model.Request;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CrewEquipmentDomainService {

    public void issueInitialLoadout(List<EngineerCrew> crews, List<Request> assignedRequests) {
        Map<Integer, List<Request>> requestsByCrewId = assignedRequests.stream()
                .filter(request -> request.getAssignedCrew() != null)
                .collect(Collectors.groupingBy(request -> request.getAssignedCrew().getId()));

        for (EngineerCrew crew : crews) {
            List<Equipment> routeEquipment = requestsByCrewId.getOrDefault(crew.getId(), List.of()).stream()
                    .map(Request::getRequiredEquipment)
                    .filter(Objects::nonNull)
                    .toList();

            crew.issueEquipment(routeEquipment);
        }
    }
}
