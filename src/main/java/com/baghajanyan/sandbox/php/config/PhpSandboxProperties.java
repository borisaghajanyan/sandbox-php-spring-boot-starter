package com.baghajanyan.sandbox.php.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sandboxcore.php")
public class PhpSandboxProperties {
    private int maxConcurrency = 5;

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

}
