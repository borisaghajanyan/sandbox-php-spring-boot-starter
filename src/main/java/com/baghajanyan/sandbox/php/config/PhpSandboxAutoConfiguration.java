package com.baghajanyan.sandbox.php.config;

import java.util.concurrent.Semaphore;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.baghajanyan.sandbox.php.executor.PhpCodeExecutor;

@AutoConfiguration
@EnableConfigurationProperties(PhpSandboxProperties.class)
public class PhpSandboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PhpCodeExecutor phpCodeExecutor(PhpSandboxProperties properties) {
        return new PhpCodeExecutor(new Semaphore(properties.getMaxConcurrency()));
    }
}
