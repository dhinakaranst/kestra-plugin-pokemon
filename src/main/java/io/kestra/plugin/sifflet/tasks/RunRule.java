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
    @PluginProperty(dynamic = true, sensitive = true)
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

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

        // Start the rule execution
        HttpRequest startRequest = HttpRequest.newBuilder()
            .uri(URI.create(resolvedUrl + "/api/v1/rules/" + resolvedRuleId + "/run"))
            .header("Authorization", "Bearer " + resolvedApiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> startResponse = client.send(startRequest, HttpResponse.BodyHandlers.ofString());
        
        if (startResponse.statusCode() != 200) {
            throw new Exception("Failed to start rule execution: " + startResponse.body());
        }

        // Parse the execution ID from the response
        ExecutionResponse executionResponse;
        try {
            executionResponse = MAPPER.readValue(startResponse.body(), ExecutionResponse.class);
        } catch (Exception e) {
            throw new Exception("Failed to parse response: " + e.getMessage(), e);
        }
        String executionId = executionResponse.executionId;

        // Poll for completion
        boolean completed = false;
        String status = "";
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeout * 1000L;

        while (!completed) {
            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                throw new Exception("Rule execution timed out after " + timeout + " seconds");
            }

            HttpRequest statusRequest = HttpRequest.newBuilder()
                .uri(URI.create(resolvedUrl + "/api/v1/executions/" + executionId))
                .header("Authorization", "Bearer " + resolvedApiKey)
                .GET()
                .build();

            HttpResponse<String> statusResponse = client.send(statusRequest, HttpResponse.BodyHandlers.ofString());
            
            if (statusResponse.statusCode() != 200) {
                throw new Exception("Failed to get execution status: " + statusResponse.body());
            }

            ExecutionStatus executionStatus;
            try {
                executionStatus = MAPPER.readValue(statusResponse.body(), ExecutionStatus.class);
            } catch (Exception e) {
                throw new Exception("Failed to parse status response: " + e.getMessage(), e);
            }
            status = executionStatus.status;
            
            if (status.equals("COMPLETED") || status.equals("FAILED")) {
                completed = true;
            } else {
                Thread.sleep(pollingInterval * 1000L);
            }
        }

        // Record metrics
        runContext.metric(Counter.of("rule.status", 1, "status", status));

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