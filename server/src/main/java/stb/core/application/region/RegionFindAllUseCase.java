package stb.core.application.region;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.dto.RegionDto;
import stb.core.domain.repository.RegionRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RegionFindAllUseCase {

    private final RegionRepository regionRepository;

    @Transactional(readOnly = true)
    public List<RegionDto> execute() {
        return regionRepository.findAllByParentRegionIsNull(Sort.by("displayName"))
                .stream()
                .map(RegionDto::from)
                .toList();
    }
}
