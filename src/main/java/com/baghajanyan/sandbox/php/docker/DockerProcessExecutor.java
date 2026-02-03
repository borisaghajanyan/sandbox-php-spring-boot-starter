package com.baghajanyan.sandbox.php.docker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import com.baghajanyan.sandbox.php.config.DockerConfig;
import com.baghajanyan.sandbox.php.docker.DockerProcessException.DockerProcessThreadException;
import com.baghajanyan.sandbox.php.docker.DockerProcessException.DockerProcessTimeoutException;

/**
 * Executes a PHP script in a sandboxed Docker container.
 * <p>
 * This class is responsible for creating and running a Docker process with
 * specified resource limits
 * and execution timeouts. It uses a {@link DockerConfig} object to configure the
 * container.
 */
public class DockerProcessExecutor {
    private final DockerConfig dockerConfig;

    public DockerProcessExecutor(DockerConfig dockerConfig) {
        this.dockerConfig = dockerConfig;
    }

    /**
     * Executes the PHP script in a Docker container.
     *
     * @param tmpFile the temporary file containing the PHP script to execute.
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
                throw new DockerProcessTimeoutException(
                        "Execution timed out after " + dockerConfig.executionTimeout().toSeconds() + " seconds");
            }
            return process;
        } catch (IOException | InterruptedException e) {
            throw new DockerProcessThreadException("Failed to execute Docker process", e);
        }
    }

    private ProcessBuilder create(Path tmpFile) {
        return new ProcessBuilder("docker", "run", "--rm", "-m", dockerConfig.maxMemoryMb() + "m",
                "--cpus=" + dockerConfig.maxCpuUnits(), "-v", tmpFile.getParent() + ":/code", dockerConfig.dockerImage(),
                "php",
                "-d", "display_errors=stderr", "-d", "error_reporting=E_ALL", "/code/" + tmpFile.getFileName());
    }
}
