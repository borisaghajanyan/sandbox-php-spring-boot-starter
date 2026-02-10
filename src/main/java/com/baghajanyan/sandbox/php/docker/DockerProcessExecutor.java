package com.baghajanyan.sandbox.php.docker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baghajanyan.sandbox.php.config.DockerConfig;
import com.baghajanyan.sandbox.php.docker.DockerProcessException.DockerProcessThreadException;
import com.baghajanyan.sandbox.php.docker.DockerProcessException.DockerProcessTimeoutException;

/**
 * Executes a script from a file in a sandboxed Docker container.
 *
 * This class is responsible for creating and running a Docker process with
 * specified resource limits and execution timeouts. It uses a
 * {@link DockerConfig} object to configure the container.
 */
public class DockerProcessExecutor {
    private static final Logger logger = LoggerFactory.getLogger(DockerProcessExecutor.class);
    private final DockerConfig dockerConfig;

    public DockerProcessExecutor(DockerConfig dockerConfig) {
        this.dockerConfig = dockerConfig;
    }

    /**
     * Executes the script from a temporary file in a Docker container.
     *
     * @param tmpFile the temporary file containing the script to execute.
     * @return the completed {@link Process} object.
     * @throws DockerProcessThreadException  if the Docker process fails to start or
     *                                       is interrupted.
     * @throws DockerProcessTimeoutException if the execution times out.
     */
    public Process execute(Path tmpFile) throws DockerProcessThreadException, DockerProcessTimeoutException {
        try {
            var builder = create(tmpFile);
            var process = builder.start();
            boolean finished = process.waitFor(dockerConfig.executionTimeout().toMillis(), TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                logger.warn("Docker process timed out after {} seconds",
                        dockerConfig.executionTimeout().toSeconds());
                throw new DockerProcessTimeoutException(
                        "Execution timed out after " + dockerConfig.executionTimeout().toSeconds() + " seconds");
            }
            return process;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.error("Failed to execute Docker process", e);
            throw new DockerProcessThreadException("Failed to execute Docker process", e);
        }
    }

    private ProcessBuilder create(Path tmpFile) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("--rm");

        if (dockerConfig.securityHardening()) {
            if (!dockerConfig.allowNetwork()) {
                command.add("--network=none");
            }
            if (dockerConfig.readOnly()) {
                command.add("--read-only");
                command.add("--tmpfs");
                command.add("/tmp:rw,noexec,nosuid,size=" + dockerConfig.tmpfsSize());
            }
            if (dockerConfig.pidsLimit() > 0) {
                command.add("--pids-limit=" + dockerConfig.pidsLimit());
            }
            if (dockerConfig.dropCapabilities()) {
                command.add("--cap-drop=ALL");
            }
            if (dockerConfig.noNewPrivileges()) {
                command.add("--security-opt");
                command.add("no-new-privileges");
            }
        }
        if (!dockerConfig.runAsUser().isBlank()) {
            command.add("--user");
            command.add(dockerConfig.runAsUser());
        }

        command.add("-m");
        command.add(dockerConfig.maxMemoryMb() + "m");
        command.add("--cpus=" + dockerConfig.maxCpuUnits());

        String volumeSuffix = dockerConfig.securityHardening() && dockerConfig.readOnly() ? ":ro" : "";
        command.add("-v");
        command.add(tmpFile.getParent() + ":/code" + volumeSuffix);

        command.add(dockerConfig.dockerImage());
        command.add("php");
        command.add("-d");
        command.add("display_errors=stderr");
        command.add("-d");
        command.add("error_reporting=E_ALL");
        command.add("/code/" + tmpFile.getFileName());

        return new ProcessBuilder(command);
    }
}
