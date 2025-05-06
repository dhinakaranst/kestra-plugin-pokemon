package io.kestra.plugin.sifflet.tasks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
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
import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List Sifflet rules",
    description = "Retrieve a list of rules from Sifflet."
)
@Plugin(
    examples = {
        @Example(
            title = "List Sifflet rules",
            code = {
                """
                id: list-sifflet-rules
                type: io.kestra.plugin.sifflet.tasks.ListRules
                url: https://api.siffletdata.com
                apiKey: "{{ secret('SIFFLET_API_KEY') }}"
                """
            }
        )
    }
)
public class ListRules extends Task implements RunnableTask<ListRules.Output> {
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
        title = "Page Size",
        description = "Number of rules to return per page"
    )
    @PluginProperty(dynamic = true)
    @Builder.Default
    private Integer pageSize = 100;

    @Schema(
        title = "Page Number",
        description = "Page number to retrieve"
    )
    @PluginProperty(dynamic = true)
    @Builder.Default
    private Integer pageNumber = 1;

    @Override
    public ListRules.Output run(RunContext runContext) {
        String resolvedUrl = runContext.render(url);
        String resolvedApiKey = runContext.render(apiKey);
        Integer resolvedPageSize = pageSize != null ? pageSize : 100;
        Integer resolvedPageNumber = pageNumber != null ? pageNumber : 1;

        if (resolvedUrl == null || resolvedUrl.isEmpty()) {
            throw new IllegalArgumentException("Sifflet API URL must be provided");
        }
        if (resolvedApiKey == null || resolvedApiKey.isEmpty()) {
            throw new IllegalArgumentException("Sifflet API key must be provided");
        }

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

        String uri = String.format("%s/api/v1/rules?pageSize=%s&pageNumber=%s",
            resolvedUrl,
            java.net.URLEncoder.encode(resolvedPageSize.toString(), java.nio.charset.StandardCharsets.UTF_8),
            java.net.URLEncoder.encode(resolvedPageNumber.toString(), java.nio.charset.StandardCharsets.UTF_8)
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .header("Authorization", "Bearer " + resolvedApiKey)
            .header("Content-Type", "application/json")
            .GET()
            .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to send request to Sifflet API: " + e.getMessage(), e);
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to list rules: " + response.body());
        }

        try {
            RulesResponse rulesResponse = MAPPER.readValue(response.body(), RulesResponse.class);
            return Output.builder()
                .rules(rulesResponse.rules)
                .totalCount(rulesResponse.totalCount)
                .pageSize(rulesResponse.pageSize)
                .pageNumber(rulesResponse.pageNumber)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse rules response: " + e.getMessage(), e);
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Rules",
            description = "List of rules"
        )
        private List<Rule> rules;

        @Schema(
            title = "Total Count",
            description = "Total number of rules"
        )
        private Integer totalCount;

        @Schema(
            title = "Page Size",
            description = "Number of rules per page"
        )
        private Integer pageSize;

        @Schema(
            title = "Page Number",
            description = "Current page number"
        )
        private Integer pageNumber;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rule {
        private String id;
        private String name;
        private String description;
        private String status;
        private String createdAt;
        private String updatedAt;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RulesResponse {
        private List<Rule> rules;
        private Integer totalCount;
        private Integer pageSize;
        private Integer pageNumber;
    }
} 