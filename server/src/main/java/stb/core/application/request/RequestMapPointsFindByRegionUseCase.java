package stb.core.application.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.dto.RequestMapPointDto;
import stb.core.domain.exception.RegionNotFoundException;
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

        return requestRepository
                .findAllByRegionId(regionId)
                .stream()
                .map(RequestMapPointDto::from)
                .toList();
    }
}
