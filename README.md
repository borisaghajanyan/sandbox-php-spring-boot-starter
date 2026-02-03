# sandbox-php-spring-boot-starter

Spring Boot starter for executing PHP code in a secure Docker sandbox, integrating [sandbox-core](https://github.com/borisaghajanyan/sandbox-core) library with auto-configuration and configurable concurrency.

## Introduction

The `sandbox-php-spring-boot-starter` provides a convenient way to integrate PHP code execution into your Spring Boot applications. It leverages Docker to create isolated and secure environments for running PHP scripts, preventing untrusted code from affecting your host system. This starter builds upon the `sandbox-core` library, offering seamless auto-configuration and easy customization of execution parameters.

## Features

*   **Secure Sandboxing:** Executes PHP code within isolated Docker containers.
*   **Resource Management:** Configurable limits for CPU and memory usage for each PHP execution.
*   **Concurrency Control:** Manages the number of simultaneous PHP executions to prevent system overload.
*   **Execution Timeout:** Prevents long-running or infinite loops from consuming excessive resources.
*   **Auto-configuration:** Seamless integration with Spring Boot's auto-configuration mechanism.
*   **Temporary File Management:** Handles the creation and deletion of temporary PHP script files.

## Getting Started

To use this starter in your Spring Boot project, add the following Maven dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.baghajanyan</groupId>
    <artifactId>sandbox-php-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version> <!-- Use the appropriate version -->
</dependency>
```

This starter also depends on the `sandbox-core` library for its core sandboxing functionalities.

## Configuration

You can customize the behavior of the PHP sandbox using properties in your `application.properties` or `application.yml` file. If no properties are explicitly set, the default values listed below will be used.

| Property                                     | Description                                                                     | Default Value        |
| :------------------------------------------- | :------------------------------------------------------------------------------ | :------------------- |
| `sandboxcore.php.max-concurrency`            | Maximum number of concurrent PHP executions.                                    | `5`                  |
| `sandboxcore.php.max-memory-mb`              | Maximum memory (in MB) allocated to the Docker container for each execution.    | `16`                 |
| `sandboxcore.php.max-cpu-units`              | Maximum CPU units allocated to the Docker container (e.g., `0.125` for 12.5% of one CPU). | `0.125`              |
| `sandboxcore.php.max-execution-time`         | Maximum time allowed for a single PHP script execution (e.g., `15s`).           | `15s` (15 seconds)   |
| `sandboxcore.php.docker-image`               | The Docker image to use for PHP execution.                                      | `php:8.2-cli`        |
| `sandboxcore.filemanager.delete.max-retries` | Maximum retries for deleting temporary files.                                   | `5`                  |
| `sandboxcore.filemanager.delete.retry-delay` | Delay between retry attempts for file deletion (e.g., `100ms`).                 | `100ms`              |
| `sandboxcore.filemanager.delete.termination-timeout` | Timeout for forcibly terminating file deletion (e.g., `500ms`).                 | `500ms`              |

**Example `application.yml`:**

```yaml
sandboxcore:
  php:
    max-concurrency: 10
    max-memory-mb: 32
    max-cpu-units: 0.5
    max-execution-time: 20s
    docker-image: php:8.3-cli
  filemanager:
    delete:
      max-retries: 3
      retry-delay: 50ms
      termination-timeout: 200ms
```

## Usage

Once configured, you can inject the `PhpCodeExecutor` bean into your Spring components and use it to execute PHP code.

```java
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

    public String executePhpCode(String phpCode) {
        CodeSnippet snippet = new CodeSnippet(phpCode);
        ExecutionResult result = phpCodeExecutor.execute(snippet);

        if (result.exitCode() == 0) {
            return result.stdout();
        } else {
            return "Error (Exit Code: " + result.exitCode() + "):\n" + result.stderr();
        }
    }
}
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
