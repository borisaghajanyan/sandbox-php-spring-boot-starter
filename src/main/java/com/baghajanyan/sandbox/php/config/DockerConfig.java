package com.baghajanyan.sandbox.php.config;

import java.time.Duration;

/**
 * Represents the configuration for a Docker container used for sandboxed
 * execution.
 * This record holds settings such as memory limits, CPU allocation, execution
 * timeouts, and the Docker image to be used.
 *
 * @param maxMemoryMb      the maximum memory allocated to the container in
 *                         megabytes.
 * @param maxCpuUnits      the maximum CPU units allocated to the container.
 * @param executionTimeout the maximum time allowed for code execution.
 * @param dockerImage      the name of the Docker image to be used for the
 *                         sandbox.
 */
public record DockerConfig(int maxMemoryMb, double maxCpuUnits, Duration executionTimeout, String dockerImage) {
    public DockerConfig {
        if (maxMemoryMb <= 0) {
            throw new IllegalArgumentException("maxMemoryMb must be greater than 0");
        }
        if (maxCpuUnits <= 0) {
            throw new IllegalArgumentException("maxCpuUnits must be greater than 0");
        }
        if (executionTimeout == null || executionTimeout.isNegative() || executionTimeout.isZero()) {
            throw new IllegalArgumentException("executionTimeout must be a positive duration");
        }
        if (dockerImage == null) {
            throw new IllegalArgumentException("dockerImage must not be null");
        }
    }
}
