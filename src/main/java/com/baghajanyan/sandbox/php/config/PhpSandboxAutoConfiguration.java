package com.baghajanyan.sandbox.php.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.baghajanyan.sandbox.core.executor.CodeExecutor;
import com.baghajanyan.sandbox.php.executor.PhpCodeExecutor;

@AutoConfiguration
public class PhpSandboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CodeExecutor phpCodeExecutor() {
        return new PhpCodeExecutor();
    }
}
