package io.kestra.plugin.templates;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;

@SuperBuilder
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@Plugin(
    examples = {
        @Example(
            title = "Run a Sifflet rule",
            code = {
                "id: run-rule",
                "type: io.kestra.plugin.templates.RunRule",
                "apiKey: \"{{ secret('SIFFLET_API_KEY') }}\"",
                "ruleId: \"data-quality-rule-123\""
            }
        )
    }
)
public class RunRule extends Task implements RunnableTask<RunRule.Output> {
    
    @PluginProperty(dynamic = true)
    @NotNull
    @NotBlank
    private String apiKey;

    @PluginProperty(dynamic = true)
    @NotNull
    @NotBlank
    private String ruleId;

    @Override
    public RunRule.Output run(RunContext runContext) throws IllegalVariableEvaluationException {
        String apiUrl = "https://api.siffletdata.com/v1/rules/" + ruleId + "/execute";
        
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(apiUrl);
            
            // Set headers
            request.setHeader("Authorization", "Bearer " + apiKey);
            request.setHeader("Content-Type", "application/json");
            
            // Set empty body for rule execution
            request.setEntity(new StringEntity("{}", ContentType.APPLICATION_JSON));
            
            // Execute request
            try {
                return client.execute(request, response -> {
                    int statusCode = response.getCode();
                    
                    if (statusCode >= 200 && statusCode < 300) {
                        return Output.builder()
                            .status("success")
                            .message("Rule executed successfully")
                            .build();
                    } else {
                        throw new IllegalStateException("Failed to execute rule. Status code: " + statusCode);
                    }
                });
            } catch (IllegalStateException e) {
                throw new IllegalVariableEvaluationException(e.getMessage());
            }
        } catch (IOException e) {
            throw new IllegalVariableEvaluationException("Failed to execute Sifflet rule: " + e.getMessage(), e);
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @NotNull
        private final String status;

        @NotNull
        private final String message;
    }
}
