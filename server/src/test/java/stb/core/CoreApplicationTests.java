package stb.core;

import com.graphhopper.GraphHopper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class CoreApplicationTests {

	@MockitoBean
	private GraphHopper graphHopper;

	@Test
	void contextLoads() {
	}

}
