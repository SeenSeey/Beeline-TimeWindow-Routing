package stb.core.application.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.dto.RequestMapPointDto;
import stb.core.domain.exception.RegionNotFoundException;
import stb.core.domain.model.Request;
import stb.core.domain.model.RequestStatus;
import stb.core.domain.repository.RegionRepository;
import stb.core.domain.repository.RequestRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RequestMapPointsFindByRegionUseCase {

    private final RegionRepository regionRepository;
    private final RequestRepository requestRepository;

    @Transactional(readOnly = true)
    public List<RequestMapPointDto> execute(int regionId) {
        if (!regionRepository.existsById(regionId)) {
            throw new RegionNotFoundException("Регион не существует");
        }

        // the map shows work still to do: completed and canceled requests are left out
        return requestRepository
                .findAllByRegionId(regionId)
                .stream()
                .filter(RequestMapPointsFindByRegionUseCase::isOpen)
                .map(RequestMapPointDto::from)
                .toList();
    }

    /** neither completed nor canceled — by its status, and by the timestamps in case the status lags behind */
    private static boolean isOpen(Request request) {
        RequestStatus.Keyword status = request.getStatus() == null ? null : request.getStatus().getKeyword();
        return status != RequestStatus.Keyword.COMPLETED
                && status != RequestStatus.Keyword.CANCELED
                && !request.isCompleted()
                && !request.isCanceled();
    }
}
