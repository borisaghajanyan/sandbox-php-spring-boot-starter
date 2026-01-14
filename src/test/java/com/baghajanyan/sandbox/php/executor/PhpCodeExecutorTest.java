package com.baghajanyan.sandbox.php.executor;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Semaphore;

import org.junit.jupiter.api.Test;

import com.baghajanyan.sandbox.core.model.CodeSnippet;

class PhpCodeExecutorTest {
    @Test
    void execute() {
        var result = executor().execute(new CodeSnippet("echo 'Hello World!';", "php"));

        assertAll(
                () -> assertEquals(0, result.exitCode()),
                () -> assertEquals("Hello World!", result.stdout()),
                () -> assertEquals("", result.stderr()));
    }

    @Test
    void execute_withPHPTags() {
        var result = executor().execute(new CodeSnippet("<?php echo 'Hello World!';", "php"));

        assertAll(
                () -> assertEquals(0, result.exitCode()),
                () -> assertEquals("Hello World!", result.stdout()),
                () -> assertEquals("", result.stderr()));
    }

    @Test
    void execute_invalidCode_ParseError() {
        var result = executor().execute(new CodeSnippet("<?php echo 'Hello World!'", "php"));

        assertAll(
                () -> assertEquals(255, result.exitCode()),
                () -> assertTrue(result.stderr().contains("Parse error: syntax error")));
    }

    @Test
    void execute_invalidCode_FatalError() {
        var result = executor().execute(new CodeSnippet("<?php $x = 1/0;", "php"));

        assertAll(
                () -> assertEquals(255, result.exitCode()),
                () -> assertTrue(result.stderr().contains("Fatal error: Uncaught DivisionByZeroError:")));
    }

    private PhpCodeExecutor executor() {
        return new PhpCodeExecutor(new Semaphore(5));
    }
}
