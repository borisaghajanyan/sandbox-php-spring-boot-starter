package com.baghajanyan.sandbox.php.config;

import java.util.concurrent.Semaphore;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.baghajanyan.sandbox.core.fs.DeleteConfig;
import com.baghajanyan.sandbox.core.fs.TempFileManager;
import com.baghajanyan.sandbox.php.docker.DockerProcessExecutor;
import com.baghajanyan.sandbox.php.executor.PhpCodeExecutor;

/**
 * Auto-configuration for the PHP sandbox environment.
 * <p>
 * This class sets up the necessary beans for running PHP code in a sandboxed
 * environment,
 * including beans for managing temporary files, controlling concurrent
 * executions,
 * and configuring the Docker container.
 */
@AutoConfiguration
@EnableConfigurationProperties({ PhpSandboxProperties.class, PhpDeleteFileManagerProperties.class })
public class PhpSandboxAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    private TempFileManager phpTempFileManager(PhpDeleteFileManagerProperties fileDeleteManagerProperties) {
        DeleteConfig deleteConfig = new DeleteConfig(fileDeleteManagerProperties.getMaxRetries(),
                fileDeleteManagerProperties.getRetryDelay(),
                fileDeleteManagerProperties.getTerminationTimeout());

        return new TempFileManager(deleteConfig);
    }

    @Bean
    @ConditionalOnMissingBean
    private Semaphore phpExecutionSemaphore(PhpSandboxProperties sandboxProperties) {
        return new Semaphore(sandboxProperties.getMaxConcurrency(), true);
    }

    @Bean
    @ConditionalOnMissingBean
    private DockerConfig phpDockerConfig(PhpSandboxProperties sandboxProperties) {
        return new DockerConfig(sandboxProperties.getMaxMemoryMb(), sandboxProperties.getMaxCpuUnits(),
                sandboxProperties.getMaxExecutionTime(), sandboxProperties.getDockerImage());
    }

    @Bean
    @ConditionalOnMissingBean
    private DockerProcessExecutor phpDockerProcess(DockerConfig dockerConfig) {
        return new DockerProcessExecutor(dockerConfig);
    }

    @Bean
    @ConditionalOnMissingBean
    PhpCodeExecutor phpCodeExecutor(Semaphore phpExecutionSemaphore, TempFileManager phpTempFileManager,
            DockerProcessExecutor phpDockerProcess) {
        return new PhpCodeExecutor(phpExecutionSemaphore, phpTempFileManager, phpDockerProcess);
    }
}
