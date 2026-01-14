package com.baghajanyan.sandbox.php.executor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;

import com.baghajanyan.sandbox.core.executor.CodeExecutor;
import com.baghajanyan.sandbox.core.executor.ExecutionResult;
import com.baghajanyan.sandbox.core.model.CodeSnippet;

public class PhpCodeExecutor implements CodeExecutor {

    private final Semaphore semaphore;

    public PhpCodeExecutor(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    public ExecutionResult execute(CodeSnippet snippet) {
        try {
            semaphore.acquire();
            return runInDocker(snippet);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ExecutionResult(0, null, "Execution interrupted");
        } finally {
            semaphore.release();
        }
    }

    private ExecutionResult runInDocker(CodeSnippet snippet) {
        Path tmpFile = null;

        try {
            tmpFile = Files.createTempFile("php-snippet-", ".php");
            var phpCode = preparePhpCode(snippet.code());
            Files.writeString(tmpFile, phpCode);

            var builder = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "-m", "128m", "--cpus=0.5",
                    "-v", tmpFile.getParent() + ":/code",
                    "php:8.2-cli",
                    "php", "-d", "display_errors=stderr",
                    "-d", "error_reporting=E_ALL",
                    "/code/" + tmpFile.getFileName());
            var process = builder.start();
            var stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
            var stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String out = stdout.lines().reduce("", (a, b) -> a + b + "\n").trim();
            String err = stderr.lines().reduce("", (a, b) -> a + b + "\n").trim();

            int exitCode = process.waitFor();

            return new ExecutionResult(exitCode, out, err);
        } catch (Exception e) {
            return new ExecutionResult(0, null, "Failed to create temp file: " + e.getMessage());
        } finally {
            try {
                if (tmpFile != null) {
                    Files.deleteIfExists(tmpFile);
                }
            } catch (Exception e) {
                // TODO: Handle cleanup exception
            }
        }
    }

    private String preparePhpCode(String code) {
        if (code.contains("<?php") || code.contains("<?")) {
            return code;
        }
        return "<?php\n" + code + "\n";
    }
}
