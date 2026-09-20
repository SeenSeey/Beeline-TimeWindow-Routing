package stb.core.application.request;

import org.springframework.stereotype.Component;
import stb.core.domain.dto.RequestSearchDto;
import stb.core.domain.repository.RequestRepository;

@Component
public class RequestSearchUseCase {

    private final RequestRepository requestRepository;

    public RequestSearchUseCase(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

//    private void validate(RequestSearchDto searchDto) {
//        if (Boolean.FALSE.equals(searchDto.hasAssignedCrew())
//                && searchDto.assignedCrewIds() != null
//                && !searchDto.assignedCrewIds().isEmpty()) {
//            throw new DomainException.ValidationException(
//                    "Нельзя одновременно искать заявки без бригады "
//                            + "и фильтровать их по ID бригад"
//            );
//        }
//    }
}
