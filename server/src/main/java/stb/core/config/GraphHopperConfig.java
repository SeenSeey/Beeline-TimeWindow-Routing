package stb.core.config;

import com.graphhopper.GraphHopper;
import com.graphhopper.config.Profile;
import com.graphhopper.util.GHUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import stb.core.infrastructure.route.GraphHopperUtils;

@Slf4j
@Configuration
public class GraphHopperConfig {

    private static final String ENCODED_VALUES = "car_access, car_average_speed, bike_access, bike_average_speed, foot_access, foot_average_speed, bike_priority, bike_road_access, mtb_rating, hike_rating, country, road_class, foot_priority, foot_road_access";

    @Bean
    public GraphHopper graphHopper(@Value("${app.route.osm-file}") String osmFile,
                                   @Value("${app.route.graph-cache}") String graphCache) {

        GraphHopper hopper = new GraphHopper();
        hopper.setGraphHopperLocation(graphCache);
        hopper.setOSMFile(osmFile);
        hopper.setEncodedValuesString(ENCODED_VALUES);
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
