package com.baghajanyan.sandbox.php.executor;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Semaphore;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.baghajanyan.sandbox.core.fs.DeleteConfig;
import com.baghajanyan.sandbox.core.fs.TempFileManager;
import com.baghajanyan.sandbox.core.model.CodeSnippet;
import com.baghajanyan.sandbox.php.config.DockerConfig;
import com.baghajanyan.sandbox.php.docker.DockerProcessExecutor;
import com.baghajanyan.sandbox.php.docker.DockerProcessException.DockerProcessThreadException;
import com.baghajanyan.sandbox.php.docker.DockerProcessException.DockerProcessTimeoutException;

class PhpCodeExecutorTestIT {

    TempFileManager fileManager = Mockito.spy(
            new TempFileManager(
                    new DeleteConfig(1, Duration.ofMillis(100), Duration.ofMillis(100))));
    Semaphore semaphore = new Semaphore(2, true);
    DockerProcessExecutor dockerProcess = Mockito
            .spy(new DockerProcessExecutor(new DockerConfig(6, 0.125, Duration.ofSeconds(5), "php:8.2-cli")));

    @Test
    void execute() {
        var executor = new PhpCodeExecutor(semaphore, fileManager, dockerProcess);

        var phpSnippet = """
                $a = 5;
                $b = 7;
                sleep(2);
                echo $a + $b;
                """;
        var snippet = new CodeSnippet(phpSnippet, Duration.ofSeconds(2), "php");
        var result = executor.execute(snippet);

        assertAll(
                () -> assertEquals("12", result.stdout()),
                () -> assertEquals("", result.stderr()),
                () -> assertEquals(0, result.exitCode()),
                () -> assertTrue(result.executionTime().compareTo(Duration.ofMillis(2100)) < 0));

        verify(fileManager).deleteAsync(any());
    }

    @Test
    void execute_whenFileCreationFails_returnFailedExecutionResult() throws Exception {
        var executor = new PhpCodeExecutor(semaphore, fileManager, dockerProcess);
        var snippet = new CodeSnippet("", Duration.ofSeconds(2), "php");

        doThrow(new IOException("Disk full")).when(fileManager).createTempFile(any(), any());

        var result = executor.execute(snippet);

        assertAll(
                () -> assertNull(result.stdout()),
                () -> assertEquals("Failed to create/write temp file: Disk full", result.stderr()),
                () -> assertEquals(0, result.exitCode()),
                () -> assertEquals(Duration.ofMillis(0), result.executionTime()));
        verify(fileManager, never()).deleteAsync(any());
    }

    @Test
    void execute_whenFileWriteFails_returnFailedExecutionResult() throws Exception {
        var executor = new PhpCodeExecutor(semaphore, fileManager, dockerProcess);
        var snippet = new CodeSnippet("", Duration.ofSeconds(2), "php");

        doThrow(new IOException("Failed to write to temp file")).when(fileManager).write(any(), any());

        var result = executor.execute(snippet);

        assertAll(
                () -> assertNull(result.stdout()),
                () -> assertEquals("Failed to create/write temp file: Failed to write to temp file", result.stderr()),
                () -> assertEquals(0, result.exitCode()),
                () -> assertEquals(Duration.ofMillis(0), result.executionTime()));
        verify(fileManager).deleteAsync(any());
    }

    @Test
    void execute_whenSnippetExecutionTimesOut_returnFailedExecutionResult() throws Exception {
        var executor = new PhpCodeExecutor(semaphore, fileManager, dockerProcess);
        var snippet = new CodeSnippet("", Duration.ofSeconds(2), "php");

        doThrow(new DockerProcessTimeoutException("Execution timed out")).when(dockerProcess).execute(any());

        var result = executor.execute(snippet);

        assertAll(
                () -> assertNull(result.stdout()),
                () -> assertEquals("Snippet execution timed out: Execution timed out", result.stderr()),
                () -> assertEquals(0, result.exitCode()),
                () -> assertEquals(Duration.ofMillis(0), result.executionTime()));
        verify(fileManager).deleteAsync(any());
    }

    @Test
    void execute_whenSnippetExecutionThreadFails_returnFailedExecutionResult() throws Exception {
        var executor = new PhpCodeExecutor(semaphore, fileManager, dockerProcess);
        var snippet = new CodeSnippet("", Duration.ofSeconds(2), "php");

        doThrow(new DockerProcessThreadException("Execution failed", new RuntimeException("Some error")))
                .when(dockerProcess).execute(any());

        var result = executor.execute(snippet);

        assertAll(
                () -> assertNull(result.stdout()),
                () -> assertEquals("Failed to handle docker process: Execution failed", result.stderr()),
                () -> assertEquals(0, result.exitCode()),
                () -> assertEquals(Duration.ofMillis(0), result.executionTime()));
        verify(fileManager).deleteAsync(any());
    }

    @Test
    void execute_whenCodeSnippetIsInvalid_returnFailedExecutionResult() throws Exception {
        var executor = new PhpCodeExecutor(semaphore, fileManager, dockerProcess);
        var snippet = new CodeSnippet("invalid", Duration.ofSeconds(2), "php");

        var result = executor.execute(snippet);

        assertAll(
                () -> assertEquals("", result.stdout()),
                () -> assertTrue(result.stderr().contains("Parse error: syntax error")),
                () -> assertEquals(255, result.exitCode()),
                () -> assertTrue(result.executionTime().compareTo(Duration.ofMillis(2100)) < 0));
        verify(fileManager).deleteAsync(any());
    }
}
