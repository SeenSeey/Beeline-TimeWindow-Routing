package stb.core.config;

import com.graphhopper.GraphHopper;
import com.graphhopper.config.Profile;
import com.graphhopper.util.GHUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import stb.core.infrastructure.route.GraphHopperUtils;

@Slf4j
@Configuration
public class GraphHopperConfig {

    @Bean
    public GraphHopper graphHopper() {
        GraphHopper hopper = new GraphHopper();
        hopper.setGraphHopperLocation("graph-cache");
        hopper.setOSMFile("map/central-fed.osm.pbf");
        hopper.setEncodedValuesString("car_access, car_average_speed, bike_access, bike_average_speed, foot_access, foot_average_speed, bike_priority, bike_road_access, mtb_rating, hike_rating, country, road_class, foot_priority, foot_road_access");
        hopper.setProfiles(GraphHopperUtils.getVehicleTypes().map(this::createProfile).toList());

        log.info("Инициализация GraphHopper...");
        hopper.importOrLoad();
        log.info("Инициализация GraphHopper завершена");

        return hopper;
    }

    private Profile createProfile(String name) {
        return new Profile(name)
                .setWeighting("custom")
                .setCustomModel(GHUtility.loadCustomModelFromJar(name + ".json"));
    }
}
