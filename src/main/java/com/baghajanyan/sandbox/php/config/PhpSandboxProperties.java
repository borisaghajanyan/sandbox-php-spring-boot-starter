package com.baghajanyan.sandbox.php.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the PHP sandbox.
 * <p>
 * This class defines settings for the sandboxed execution of PHP code, including
 * concurrency limits,
 * resource allocation (memory and CPU), and execution timeouts.
 */
@ConfigurationProperties(prefix = "sandboxcore.php")
public class PhpSandboxProperties {

    /**
     * The maximum number of concurrent PHP executions.
     */
    private int maxConcurrency = 5;
    /**
     * The maximum memory in megabytes allocated to the PHP container.
     */
    private int maxMemoryMb = 16;
    /**
     * The maximum CPU units allocated to the PHP container.
     */
    private double maxCpuUnits = 0.125;
    /**
     * The maximum time allowed for a single PHP execution.
     */
    private Duration maxExecutionTime = Duration.ofMillis(15000);
    /**
     * The Docker image to use for the PHP sandbox.
     */
    private String dockerImage = "php:8.2-cli";

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    public Duration getMaxExecutionTime() {
        return maxExecutionTime;
    }

    public void setMaxExecutionTime(Duration maxExecutionTime) {
        this.maxExecutionTime = maxExecutionTime;
    }

    public int getMaxMemoryMb() {
        return maxMemoryMb;
    }

    public void setMaxMemoryMb(int maxMemoryMb) {
        this.maxMemoryMb = maxMemoryMb;
    }

    public double getMaxCpuUnits() {
        return maxCpuUnits;
    }

    public void setMaxCpuUnits(double maxCpuUnits) {
        this.maxCpuUnits = maxCpuUnits;
    }


    public String getDockerImage() {
        return dockerImage;
    }

    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }
}