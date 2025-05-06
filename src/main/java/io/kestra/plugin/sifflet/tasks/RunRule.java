package io.kestra.plugin.sifflet.tasks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.executions.metrics.Counter;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Run a Sifflet rule",
    description = "Execute a rule in Sifflet and wait for its completion."
)
@Plugin(
    examples = {
        @Example(
            title = "Run a Sifflet rule",
            code = {
                """
                id: run-sifflet-rule
                type: io.kestra.plugin.sifflet.tasks.RunRule
                url: https://api.siffletdata.com
                apiKey: "{{ secret('SIFFLET_API_KEY') }}"
                ruleId: "rule-123"
                """
            }
        )
    }
)
public class RunRule extends Task implements RunnableTask<RunRule.Output> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Schema(
        title = "Sifflet API URL",
        description = "The base URL for the Sifflet API"
    )
    @PluginProperty(dynamic = true)
    private String url;

    @Schema(
        title = "API Key",
        description = "The API key for authentication with Sifflet"
    )
    @PluginProperty(dynamic = true)
    private String apiKey;

    @Schema(
        title = "Rule ID",
        description = "The ID of the rule to run"
    )
    @PluginProperty(dynamic = true)
    private String ruleId;

    @Schema(
        title = "Polling Interval",
        description = "The interval in seconds between status checks"
    )
    @PluginProperty(dynamic = true)
    @Builder.Default
    private Integer pollingInterval = 5;

    @Schema(
        title = "Timeout",
        description = "The maximum time in seconds to wait for the rule to complete"
    )
    @PluginProperty(dynamic = true)
    @Builder.Default
    private Integer timeout = 3600;

    @Override
    public RunRule.Output run(RunContext runContext) throws Exception {
        String resolvedUrl = runContext.render(url);
        String resolvedApiKey = runContext.render(apiKey);
        String resolvedRuleId = runContext.render(ruleId);

        if (resolvedUrl == null || resolvedUrl.isEmpty()) {
            throw new IllegalArgumentException("Sifflet API URL must be provided");
        }
        if (resolvedApiKey == null || resolvedApiKey.isEmpty()) {
            throw new IllegalArgumentException("Sifflet API key must be provided");
        }
        if (resolvedRuleId == null || resolvedRuleId.isEmpty()) {
            throw new IllegalArgumentException("Sifflet ruleId must be provided");
        }

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(resolvedUrl + "/api/v1/rules/" + resolvedRuleId + "/run"))
            .header("Authorization", "Bearer " + resolvedApiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to start rule execution: " + response.body());
        }

        String executionId;
        try {
            executionId = MAPPER.readTree(response.body()).get("executionId").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response: " + e.getMessage(), e);
        }

        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeout * 1000L;
        boolean completed = false;
        String status = null;
        Exception pollingException = null;
        while (!completed) {
            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                throw new RuntimeException("Rule execution timed out after " + timeout + " seconds");
            }
            try {
                Thread.sleep(pollingInterval * 1000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Polling interrupted while waiting for rule execution", ie);
            }
            HttpRequest statusRequest = HttpRequest.newBuilder()
                .uri(URI.create(resolvedUrl + "/api/v1/rules/executions/" + executionId + "/status"))
                .header("Authorization", "Bearer " + resolvedApiKey)
                .header("Content-Type", "application/json")
                .GET()
                .build();
            HttpResponse<String> statusResponse;
            try {
                statusResponse = client.send(statusRequest, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                pollingException = e;
                break;
            }
            if (statusResponse.statusCode() != 200) {
                throw new RuntimeException("Failed to check rule execution status: " + statusResponse.body());
            }
            try {
                status = MAPPER.readTree(statusResponse.body()).get("status").asText();
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse response: " + e.getMessage(), e);
            }
            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                completed = true;
            }
        }
        if (pollingException != null) {
            throw new RuntimeException("Polling failed: " + pollingException.getMessage(), pollingException);
        }
        return Output.builder()
            .executionId(executionId)
            .status(status)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Execution ID",
            description = "The ID of the rule execution"
        )
        private String executionId;

        @Schema(
            title = "Status",
            description = "The final status of the rule execution"
        )
        private String status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ExecutionResponse {
        private String executionId;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ExecutionStatus {
        private String status;
    }
} 