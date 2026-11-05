package net.diveon.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "DIVEON_CONTEXT_TEST", matches = "approved")
@SpringBootTest
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
