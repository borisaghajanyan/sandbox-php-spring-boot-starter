# sandbox-php-spring-boot-starter

A Spring Boot starter library that provides a secure PHP sandbox.  
It allows other Spring Boot services to execute PHP code snippets inside isolated Docker containers, with support for:

- Syntax validation
- Capturing standard output and error
- Limiting concurrent executions (max 5 containers by default)
- Easy integration with your Spring Boot applications

---

## Features

- **Secure**: Runs PHP snippets in Docker containers to prevent code from affecting the host.
- **Language-specific starter**: Extends `sandbox-core` for PHP execution.
- **Configurable**: Semaphore limit and fail-on-stderr behavior can be customized.
- **Lightweight**: Only depends on `sandbox-core` and Spring Boot testing libraries.

---

## Installation

Add this dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.baghajanyan</groupId>
    <artifactId>sandbox-php-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```
