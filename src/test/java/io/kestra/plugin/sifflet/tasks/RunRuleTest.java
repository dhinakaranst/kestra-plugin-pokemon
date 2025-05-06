package io.kestra.plugin.sifflet.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
class RunRuleTest {
    @Inject
    private RunContextFactory runContextFactory;

    private MockWebServer mockWebServer;
    private ObjectMapper objectMapper;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        baseUrl = "http://localhost:" + mockWebServer.getPort();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testSuccessfulRuleExecution() throws Exception {
        // Mock successful rule start
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(objectMapper.writeValueAsString(Map.of("executionId", "test-execution-123"))));

        // Mock successful status check
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(objectMapper.writeValueAsString(Map.of("status", "COMPLETED"))));

        RunRule task = RunRule.builder()
            .id(IdUtils.create())
            .type(RunRule.class.getName())
            .url(baseUrl)
            .apiKey("test-api-key")
            .ruleId("test-rule-id")
            .pollingInterval(1)
            .timeout(10)
            .build();

        RunContext runContext = runContextFactory.of();
        RunRule.Output output = task.run(runContext);

        assertThat(output, is(notNullValue()));
        assertThat(output.getExecutionId(), is("test-execution-123"));
        assertThat(output.getStatus(), is("COMPLETED"));
    }

    @Test
    void testFailedRuleExecution() throws Exception {
        // Mock successful rule start
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(objectMapper.writeValueAsString(Map.of("executionId", "test-execution-123"))));

        // Mock failed status check
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(objectMapper.writeValueAsString(Map.of("status", "FAILED"))));

        RunRule task = RunRule.builder()
            .id(IdUtils.create())
            .type(RunRule.class.getName())
            .url(baseUrl)
            .apiKey("test-api-key")
            .ruleId("test-rule-id")
            .pollingInterval(1)
            .timeout(10)
            .build();

        RunContext runContext = runContextFactory.of();
        RunRule.Output output = task.run(runContext);

        assertThat(output, is(notNullValue()));
        assertThat(output.getExecutionId(), is("test-execution-123"));
        assertThat(output.getStatus(), is("FAILED"));
    }

    @Test
    void testApiError() {
        // Mock API error
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(401)
            .setBody("Unauthorized"));

        RunRule task = RunRule.builder()
            .id(IdUtils.create())
            .type(RunRule.class.getName())
            .url(baseUrl)
            .apiKey("invalid-api-key")
            .ruleId("test-rule-id")
            .pollingInterval(1)
            .timeout(10)
            .build();

        RunContext runContext = runContextFactory.of();
        Exception exception = assertThrows(Exception.class, () -> task.run(runContext));
        assertThat(exception.getMessage(), containsString("Failed to start rule execution"));
    }

    @Test
    void testTimeout() throws Exception {
        // Mock successful rule start
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(objectMapper.writeValueAsString(Map.of("executionId", "test-execution-123"))));

        // Enqueue multiple RUNNING responses to simulate polling
        for (int i = 0; i < 3; i++) {
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(Map.of("status", "RUNNING"))));
        }

        RunRule task = RunRule.builder()
            .id(IdUtils.create())
            .type(RunRule.class.getName())
            .url(baseUrl)
            .apiKey("test-api-key")
            .ruleId("test-rule-id")
            .pollingInterval(1)
            .timeout(1) // Set a very short timeout
            .build();

        RunContext runContext = runContextFactory.of();
        Exception exception = assertThrows(Exception.class, () -> task.run(runContext));
        assertThat(exception.getMessage(), containsString("Rule execution timed out"));
    }

    @Test
    void testMetricsRecorded() throws Exception {
        // Mock successful rule start
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(objectMapper.writeValueAsString(Map.of("executionId", "test-execution-123"))));
        // Mock successful status check
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(objectMapper.writeValueAsString(Map.of("status", "COMPLETED"))));

        RunRule task = RunRule.builder()
            .id(IdUtils.create())
            .type(RunRule.class.getName())
            .url(baseUrl)
            .apiKey("test-api-key")
            .ruleId("test-rule-id")
            .pollingInterval(1)
            .timeout(10)
            .build();

        RunContext runContext = runContextFactory.of();
        task.run(runContext);
        // Check that metrics are recorded (example: check for a metric key)
        assertThat(runContext.metrics().size(), greaterThan(0));
    }

    @Test
    void testStatusCheckApiError() throws Exception {
        // Mock successful rule start
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(objectMapper.writeValueAsString(Map.of("executionId", "test-execution-123"))));
        // Mock status check API error
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error"));

        RunRule task = RunRule.builder()
            .id(IdUtils.create())
            .type(RunRule.class.getName())
            .url(baseUrl)
            .apiKey("test-api-key")
            .ruleId("test-rule-id")
            .pollingInterval(1)
            .timeout(10)
            .build();

        RunContext runContext = runContextFactory.of();
        Exception exception = assertThrows(Exception.class, () -> task.run(runContext));
        assertThat(exception.getMessage(), containsString("Failed to check rule execution status"));
    }

    @Test
    void testInvalidJsonStatusResponse() throws Exception {
        // Mock successful rule start
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(objectMapper.writeValueAsString(Map.of("executionId", "test-execution-123"))));
        // Mock invalid JSON for status check
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("invalid json"));

        RunRule task = RunRule.builder()
            .id(IdUtils.create())
            .type(RunRule.class.getName())
            .url(baseUrl)
            .apiKey("test-api-key")
            .ruleId("test-rule-id")
            .pollingInterval(1)
            .timeout(10)
            .build();

        RunContext runContext = runContextFactory.of();
        Exception exception = assertThrows(Exception.class, () -> task.run(runContext));
        assertThat(exception.getMessage(), containsString("Failed to parse response"));
    }
} 