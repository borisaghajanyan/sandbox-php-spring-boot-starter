package com.baghajanyan.sandbox.php.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = { PhpSandboxAutoConfiguration.class })
@ActiveProfiles("test")
public class PhpSandboxPropertiesIT {

    @Autowired
    private PhpSandboxProperties phpSandboxProperties;

    @Autowired
    private PhpDeleteFileManagerProperties phpDeleteFileManagerProperties;

    @Test
    void phpSandboxPropertiesAreLoadedCorrectly() {
        assertNotNull(phpSandboxProperties);
        assertEquals(10, phpSandboxProperties.getMaxConcurrency());
        assertEquals(32, phpSandboxProperties.getMaxMemoryMb());
        assertEquals(0.5, phpSandboxProperties.getMaxCpuUnits());
        assertEquals(Duration.ofSeconds(20), phpSandboxProperties.getMaxExecutionTime());
        assertEquals("php:8.3-cli-test", phpSandboxProperties.getDockerImage());
    }

    @Test
    void phpDeleteFileManagerPropertiesAreLoadedCorrectly() {
        assertNotNull(phpDeleteFileManagerProperties);
        assertEquals(3, phpDeleteFileManagerProperties.getMaxRetries());
        assertEquals(Duration.ofMillis(50), phpDeleteFileManagerProperties.getRetryDelay());
        assertEquals(Duration.ofMillis(200), phpDeleteFileManagerProperties.getTerminationTimeout());
    }
}
