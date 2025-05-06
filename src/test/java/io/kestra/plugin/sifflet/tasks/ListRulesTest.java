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
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
class ListRulesTest {
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
    void testListRules() throws Exception {
        // Mock successful response
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(objectMapper.writeValueAsString(Map.of(
                "rules", List.of(Map.of(
                    "id", "rule-1",
                    "name", "Test Rule",
                    "description", "Test Description",
                    "status", "ACTIVE",
                    "createdAt", "2024-02-26T10:00:00Z",
                    "updatedAt", "2024-02-26T10:00:00Z"
                )),
                "totalCount", 1,
                "pageSize", 100,
                "pageNumber", 1
            ))));

        ListRules task = ListRules.builder()
            .id(IdUtils.create())
            .type(ListRules.class.getName())
            .url(baseUrl)
            .apiKey("test-api-key")
            .build();

        RunContext runContext = runContextFactory.of();
        ListRules.Output output = task.run(runContext);

        assertThat(output, is(notNullValue()));
        assertThat(output.getRules(), hasSize(1));
        assertThat(output.getTotalCount(), is(1));
        assertThat(output.getPageSize(), is(100));
        assertThat(output.getPageNumber(), is(1));

        ListRules.Rule rule = output.getRules().get(0);
        assertThat(rule.getId(), is("rule-1"));
        assertThat(rule.getName(), is("Test Rule"));
        assertThat(rule.getDescription(), is("Test Description"));
        assertThat(rule.getStatus(), is("ACTIVE"));
    }

    @Test
    void testApiError() {
        // Mock API error
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(401)
            .setBody("Unauthorized"));

        ListRules task = ListRules.builder()
            .id(IdUtils.create())
            .type(ListRules.class.getName())
            .url(baseUrl)
            .apiKey("invalid-api-key")
            .build();

        RunContext runContext = runContextFactory.of();
        Exception exception = assertThrows(Exception.class, () -> task.run(runContext));
        assertThat(exception.getMessage(), containsString("Failed to list rules"));
    }

    @Test
    void testInvalidJsonResponse() {
        // Mock invalid JSON response
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("invalid json"));

        ListRules task = ListRules.builder()
            .id(IdUtils.create())
            .type(ListRules.class.getName())
            .url(baseUrl)
            .apiKey("test-api-key")
            .build();

        RunContext runContext = runContextFactory.of();
        Exception exception = assertThrows(Exception.class, () -> task.run(runContext));
        assertThat(exception.getMessage(), containsString("Failed to parse rules response"));
    }

    @Test
    void testAuthorizationHeaderIsSet() throws Exception {
        // Mock successful response
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(objectMapper.writeValueAsString(Map.of(
                "rules", List.of(Map.of(
                    "id", "rule-1",
                    "name", "Test Rule",
                    "description", "Test Description",
                    "status", "ACTIVE",
                    "createdAt", "2024-02-26T10:00:00Z",
                    "updatedAt", "2024-02-26T10:00:00Z"
                )),
                "totalCount", 1,
                "pageSize", 100,
                "pageNumber", 1
            ))));

        ListRules task = ListRules.builder()
            .id(IdUtils.create())
            .type(ListRules.class.getName())
            .url(baseUrl)
            .apiKey("test-api-key")
            .build();

        RunContext runContext = runContextFactory.of();
        task.run(runContext);

        // Verify the Authorization header
        var recordedRequest = mockWebServer.takeRequest();
        String authHeader = recordedRequest.getHeader("Authorization");
        assertThat(authHeader, is("Bearer test-api-key"));
    }
} 