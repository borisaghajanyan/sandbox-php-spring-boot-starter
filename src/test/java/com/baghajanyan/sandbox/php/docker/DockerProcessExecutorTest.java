package com.baghajanyan.sandbox.php.docker;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import com.baghajanyan.sandbox.php.config.DockerConfig;
import com.baghajanyan.sandbox.php.docker.DockerProcessException.DockerProcessThreadException;
import com.baghajanyan.sandbox.php.docker.DockerProcessException.DockerProcessTimeoutException;

public class DockerProcessExecutorTest {
    private DockerConfig dockerConfig() {
        DockerConfig config = mock(DockerConfig.class);
        when(config.executionTimeout()).thenReturn(Duration.ofSeconds(1));
        when(config.maxMemoryMb()).thenReturn(128);
        when(config.maxCpuUnits()).thenReturn(1.0);
        when(config.securityHardening()).thenReturn(true);
        when(config.allowNetwork()).thenReturn(false);
        when(config.readOnly()).thenReturn(true);
        when(config.pidsLimit()).thenReturn(64);
        when(config.runAsUser()).thenReturn("65534:65534");
        when(config.tmpfsSize()).thenReturn("64m");
        when(config.dropCapabilities()).thenReturn(true);
        when(config.noNewPrivileges()).thenReturn(true);
        return config;
    }

    @Test
    void execute() throws Exception {
        var config = dockerConfig();
        var executor = new DockerProcessExecutor(config);

        var process = mock(Process.class);
        when(process.waitFor(anyLong(), any())).thenReturn(true);

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (builder, context) -> when(builder.start()).thenReturn(process))) {

            var result = executor.execute(Path.of("/tmp/test.php"));

            assertSame(process, result);
            verify(process).waitFor(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    @Test
    void execute_onTimeout_throwsTimeoutExceptionAndKillsProcess() throws Exception {
        var config = dockerConfig();
        var executor = new DockerProcessExecutor(config);

        var process = mock(Process.class);
        when(process.waitFor(anyLong(), any())).thenReturn(false);

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (builder, context) -> when(builder.start()).thenReturn(process))) {

            var ex = assertThrows(DockerProcessTimeoutException.class,
                    () -> executor.execute(Path.of("/tmp/test.php")));

            assertTrue(ex.getMessage().contains("Execution timed out"));
            verify(process).destroyForcibly();
        }
    }

    @Test
    void execute_whenStartThrowsIOException_wrapInThreadException() throws Exception {
        var config = dockerConfig();
        var executor = new DockerProcessExecutor(config);

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (builder, context) -> when(builder.start()).thenThrow(new IOException("boom")))) {

            var ex = assertThrows(DockerProcessThreadException.class, () -> executor.execute(Path.of("/tmp/test.php")));

            assertTrue(ex.getCause() instanceof IOException);
        }
    }

    @Test
    void execute_whenWaitInterrupted_wrapInThreadException() throws Exception {
        var config = dockerConfig();
        var executor = new DockerProcessExecutor(config);

        var process = mock(Process.class);
        when(process.waitFor(anyLong(), any()))
                .thenThrow(new InterruptedException("interrupted"));

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (builder, context) -> when(builder.start()).thenReturn(process))) {

            var ex = assertThrows(DockerProcessThreadException.class, () -> executor.execute(Path.of("/tmp/test.php")));

            assertTrue(ex.getCause() instanceof InterruptedException);
            assertTrue(Thread.currentThread().isInterrupted());
        }
        Thread.interrupted();
    }
}
