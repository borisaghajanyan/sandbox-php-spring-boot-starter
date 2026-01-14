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
- **Configurable**: The maximum number of concurrent executions can be customized.
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

---

## Configuration

The following properties can be configured in your `application.properties` or `application.yml` file:

| Property                          | Description                                  | Default Value |
| --------------------------------- | -------------------------------------------- | ------------- |
| `sandboxcore.php.max-concurrency` | The maximum number of concurrent executions. | `5`           |

### Example

Here's an example of how to configure the properties in your `application.properties` file:

```properties
sandboxcore.php.max-concurrency=10
```

---

## Usage

To use the PHP code executor, inject the `CodeExecutor` bean into your Spring component and use its `execute` method.

### Example

Here's an example of a Spring service that uses the `CodeExecutor` to execute a PHP code snippet:

```java
package com.baghajanyan.sandbox.php.demo;

import com.baghajanyan.sandbox.core.executor.CodeExecutor;
import com.baghajanyan.sandbox.core.executor.ExecutionResult;
import com.baghajanyan.sandbox.core.model.CodeSnippet;
import org.springframework.stereotype.Service;

@Service
public class PhpExecutionService {

    private final CodeExecutor phpCodeExecutor;

    public PhpExecutionService(CodeExecutor phpCodeExecutor) {
        this.phpCodeExecutor = phpCodeExecutor;
    }

    public ExecutionResult executePhpCode(String code) {
        CodeSnippet snippet = new CodeSnippet(code);
        return phpCodeExecutor.execute(snippet);
    }
}
```

In this example:

1.  The `CodeExecutor` bean is injected into the `PhpExecutionService`.
2.  The `executePhpCode` method takes a string of PHP code, creates a `CodeSnippet` object, and passes it to the `execute` method of the `CodeExecutor`.
3.  The `execute` method returns an `ExecutionResult` object, which contains the exit code, standard output, and standard error of the execution.
