# sandbox-php-spring-boot-starter

Spring Boot starter for executing PHP code in a secure, isolated Docker sandbox, integrating [sandbox-core](https://github.com/borisaghajanyan/sandbox-core) library with auto-configuration and configurable concurrency.

## Requirements

- Java 21

## Introduction

The `sandbox-php-spring-boot-starter` provides a convenient way to integrate PHP code execution into your Spring Boot applications. It leverages Docker to create secure, isolated environments for running PHP scripts. This starter builds upon the [sandbox-core](https://github.com/borisaghajanyan/sandbox-core) library, offering seamless auto-configuration and easy customization of execution parameters.

## Features

- **Secure Sandboxing:** Runs PHP code within isolated Docker containers.
- **Resource Management:** Configurable limits for CPU and memory usage for each PHP execution.
- **Concurrency Control:** Manages the number of simultaneous PHP executions to prevent system overload.
- **Execution Timeout:** Prevents long-running or infinite loops from consuming excessive resources.
- **Auto-configuration:** Seamless integration with Spring Boot's auto-configuration mechanism.
- **Temporary File Management:** Handles the creation and deletion of temporary PHP script files.

## Getting Started

To use this starter in your Spring Boot project, add the following Maven dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.baghajanyan</groupId>
    <artifactId>sandbox-php-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version> <!-- Use the appropriate version -->
</dependency>
```

## Configuration

You can customize the behavior of the PHP sandbox using properties in your `application.properties` or `application.yml` file. The executor writes each snippet to a temporary file and runs it inside Docker, so the file deletion settings control cleanup of those temporary files after execution. If no properties are explicitly set, the default values listed below will be used.

| Property                                             | Description                                                                               | Default Value      |
| :--------------------------------------------------- | :---------------------------------------------------------------------------------------- | :----------------- |
| `sandboxcore.php.max-concurrency`                    | Maximum number of concurrent PHP executions.                                              | `5`                |
| `sandboxcore.php.max-memory-mb`                      | Maximum memory (in MB) allocated to the Docker container for each execution.              | `16`               |
| `sandboxcore.php.max-cpu-units`                      | Maximum CPU units allocated to the Docker container (e.g., `0.125` for 12.5% of one CPU). | `0.125`            |
| `sandboxcore.php.max-execution-time`                 | Maximum time allowed for a single PHP script execution (e.g., `15s`).                     | `15s` (15 seconds) |
| `sandboxcore.php.docker-image`                       | The Docker image to use for PHP execution.                                                | `php:8.2-cli`      |
| `sandboxcore.php.security.enable-hardening`          | Enable hardened Docker sandbox flags.                                                     | `true`             |
| `sandboxcore.php.security.allow-network`             | Allow network access for the container.                                                   | `false`            |
| `sandboxcore.php.security.read-only`                 | Run the container with a read-only filesystem.                                            | `true`             |
| `sandboxcore.php.security.pids-limit`                | Max processes allowed inside the container.                                               | `64`               |
| `sandboxcore.php.security.run-as-user`               | User/group to run as inside the container.                                                | `65534:65534`      |
| `sandboxcore.php.security.tmpfs-size`                | Size of tmpfs mounted at `/tmp`.                                                          | `64m`              |
| `sandboxcore.php.security.drop-capabilities`         | Drop all Linux capabilities.                                                              | `true`             |
| `sandboxcore.php.security.no-new-privileges`         | Prevent privilege escalation inside the container.                                        | `true`             |
| `sandboxcore.filemanager.delete.max-retries`         | Maximum retries for deleting temporary files.                                             | `5`                |
| `sandboxcore.filemanager.delete.retry-delay`         | Delay between retry attempts for file deletion (e.g., `100ms`).                           | `100ms`            |
| `sandboxcore.filemanager.delete.termination-timeout` | Timeout for forcibly terminating file deletion (e.g., `500ms`).                           | `500ms`            |

Note: snippets are written to temporary files before execution in Docker, so these deletion settings control cleanup.

**Example `application.yml`:**

```yaml
sandboxcore:
  php:
    max-concurrency: 10
    max-memory-mb: 32
    max-cpu-units: 0.5
    max-execution-time: 20s
    docker-image: php:8.3-cli
    security:
      enable-hardening: true
      allow-network: false
      read-only: true
      pids-limit: 64
      run-as-user: "65534:65534"
      tmpfs-size: 64m
      drop-capabilities: true
      no-new-privileges: true
  filemanager:
    delete:
      max-retries: 3
      retry-delay: 50ms
      termination-timeout: 200ms
```

## Usage

Once configured, you can inject the `PhpCodeExecutor` bean into your Spring components and use it to execute PHP code. The executor can handle PHP code snippets that either include standard `<?php ... ?>` tags or are plain PHP code without them. The executor will automatically ensure the code is properly wrapped for execution.

**Example PHP Code Snippets:**

```php
<?php
    echo "Hello, World!";
?>
```

or simply:

```php
echo "Hello, World!";
$name = "Gemini";
echo "My name is " . $name;
```

**Integrating with `PhpCodeExecutor`:**

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
        CodeSnippet snippet = new CodeSnippet(phpCode, Duration.ofSeconds(2), "php");
        ExecutionResult result = phpCodeExecutor.execute(snippet);

        // Analyze the execution result
        if (result.exitCode() == 0 && (result.stderr() == null || result.stderr().isBlank())) {
            System.out.println("PHP Output: \n" + result.stdout());
            System.out.println("Execution Time: " + result.executionTime().toMillis() + " ms");
            return result.stdout();
        } else {
            System.err.println("PHP Error (Exit Code: " + result.exitCode() + "):\n" + result.stderr());
            System.err.println("Execution Time: " + result.executionTime().toMillis() + " ms");
            return "Error (Exit Code: " + result.exitCode() + "):\n" + result.stderr();
        }
    }
}
```

**Understanding `ExecutionResult`:**

The `execute` method returns an `ExecutionResult` object, which provides the following information:

- `exitCode()`: The exit status of the PHP process. A value of `0` typically indicates successful execution.
- `stdout()`: The standard output generated by the PHP script.
- `stderr()`: The standard error output generated by the PHP script, containing error messages or warnings.
- `executionTime()`: The actual time taken for the PHP script to execute within the sandbox, as a `java.time.Duration`.

## Security Notes

Containers are started with hardened flags (no network, read-only filesystem, dropped capabilities, no-new-privileges, PID limits, and non-root user). If you have additional requirements, consider further tightening via seccomp/AppArmor profiles or rootless Docker.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
