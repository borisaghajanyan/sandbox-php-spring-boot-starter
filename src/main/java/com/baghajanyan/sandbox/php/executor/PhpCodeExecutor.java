package com.baghajanyan.sandbox.php.executor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.baghajanyan.sandbox.core.executor.CodeExecutor;
import com.baghajanyan.sandbox.core.executor.ExecutionResult;
import com.baghajanyan.sandbox.core.fs.TempFileManager;
import com.baghajanyan.sandbox.core.model.CodeSnippet;
import com.baghajanyan.sandbox.php.docker.DockerProcessExecutor;
import com.baghajanyan.sandbox.php.docker.DockerProcessException.DockerProcessThreadException;
import com.baghajanyan.sandbox.php.docker.DockerProcessException.DockerProcessTimeoutException;

/**
 * Executes a PHP code snippet in a sandboxed environment.
 *
 * This class implements the {@link CodeExecutor} interface and is responsible
 * for executing PHP code in a Docker container. It uses a {@link Semaphore} to
 * control concurrent executions and a {@link TempFileManager} to manage
 * temporary files.
 */
public class PhpCodeExecutor implements CodeExecutor {

    private static final long EXECUTION_TIME_ZERO = 0;
    private static final int EXCEPTION_EXIT_CODE = -1;

    private final Semaphore semaphore;
    private final TempFileManager fileManager;
    private final DockerProcessExecutor process;

    public PhpCodeExecutor(Semaphore semaphore, TempFileManager fileManager, DockerProcessExecutor process) {
        this.semaphore = semaphore;
        this.fileManager = fileManager;
        this.process = process;
    }

    /**
     * Executes the given code snippet.
     *
     * @param snippet the PHP code snippet to execute.
     * @return the result of the execution.
     */
    public ExecutionResult execute(CodeSnippet snippet) {
        boolean acquired = false;
        try {
            semaphore.acquire();
            acquired = true;
            return executeInDocker(snippet);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ExecutionResult(EXCEPTION_EXIT_CODE, null, "Execution interrupted",
                    Duration.ofMillis(EXECUTION_TIME_ZERO));
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }

    private ExecutionResult executeInDocker(CodeSnippet snippet) {
        Path tmpFile = null;

        try {
            tmpFile = fileManager.createTempFile("php-snippet-" + System.nanoTime(), ".php");
            String phpCode = preparePhpCode(snippet.code());
            fileManager.write(tmpFile, phpCode);

            var dockerProcess = process.execute(tmpFile);

            return parseDockerExecutionResult(dockerProcess);
        } catch (IOException e) {
            return new ExecutionResult(EXCEPTION_EXIT_CODE, null, "Failed to create/write temp file: " + e.getMessage(),
                    Duration.ofMillis(EXECUTION_TIME_ZERO));
        } catch (DockerProcessThreadException e) {
            return new ExecutionResult(EXCEPTION_EXIT_CODE, null, "Failed to handle docker process: " + e.getMessage(),
                    Duration.ofMillis(EXECUTION_TIME_ZERO));
        } catch (DockerProcessTimeoutException e) {
            return new ExecutionResult(EXCEPTION_EXIT_CODE, null, "Snippet execution timed out: " + e.getMessage(),
                    Duration.ofMillis(EXECUTION_TIME_ZERO));
        } finally {
            if (tmpFile != null) {
                fileManager.deleteAsync(tmpFile);
            }
        }
    }

    private String preparePhpCode(String code) {
        // Remove any existing PHP tags to avoid syntax errors
        String sanitizedCode = code
                // remove opening tag only if it's at the beginning (ignoring whitespace)
                .replaceFirst("^\\s*<\\?php\\s*", "")
                // remove closing tag only if it's at the end (ignoring whitespace)
                .replaceFirst("\\s*\\?>\\s*$", "");

        return "<?php\n" +
                "$start = microtime(true);\n" +
                sanitizedCode + "\n" +
                "$end = microtime(true);\n" +
                "fwrite(STDOUT, \"\\n__EXECUTION_TIME__: \" . (($end - $start) * 1000) . \"\\n\");\n" +
                "?>";
    }

    private ExecutionResult parseDockerExecutionResult(Process dockerProcess) {
        int exitCode = dockerProcess.exitValue();
        var stdout = new BufferedReader(new InputStreamReader(dockerProcess.getInputStream()));
        var stderr = new BufferedReader(new InputStreamReader(dockerProcess.getErrorStream()));

        String out = stdout.lines().reduce("", (a, b) -> a + b + "\n").trim();
        String err = stderr.lines().reduce("", (a, b) -> a + b + "\n").trim();

        long executionTime = 0;
        Pattern pattern = Pattern.compile("__EXECUTION_TIME__:\\s*(\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(out);
        if (matcher.find()) {
            executionTime = (long) Double.parseDouble(matcher.group(1));
            out = matcher.replaceAll("").trim();
        }

        return new ExecutionResult(exitCode, out, err, Duration.ofMillis(executionTime));
    }

}
