package stb.core.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stb.core.domain.model.Region;
import stb.core.domain.repository.RegionRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionDomainService {

    private final RegionRepository regionRepository;

    public Region root(Region region) {
        if (region.getParentRegion() == null) {
            return region;
        }

        return region.getParentRegion();
    }

    public Region nearestDistrict(Region rootRegion, double latitude, double longitude) {
        List<Region> districts = new ArrayList<>(regionRepository.findAllByParentRegionId(rootRegion.getId()));
        districts.add(rootRegion);

        return nearest(districts, latitude, longitude);
    }

    public Region nearest(List<Region> districts, double latitude, double longitude) {
        Region nearestDistrict = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Region district : districts) {
            double distance = squaredDistance(district, latitude, longitude);

            if (distance < nearestDistance) {
                nearestDistrict = district;
                nearestDistance = distance;
            }
        }

        if (nearestDistrict == null) {
            throw new IllegalArgumentException("Не передано ни одного района");
        }

        return nearestDistrict;
    }

    private double squaredDistance(Region district, double latitude, double longitude) {
        double latitudeDelta = district.getOfficeLatitude() - latitude;
        double longitudeDelta = (district.getOfficeLongitude() - longitude) * Math.cos(Math.toRadians(latitude));

        return latitudeDelta * latitudeDelta + longitudeDelta * longitudeDelta;
    }
}
