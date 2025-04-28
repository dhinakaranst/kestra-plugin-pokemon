package io.kestra.plugin.templates;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.Output;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.util.HashMap;
import java.util.Map;

@SuperBuilder
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@Plugin(
    id = "run-rule",
    title = "Sifflet Run Rule",
    description = "Execute a Sifflet data quality rule",
    namespace = "io.kestra.plugin.templates"
)
@Example(
    title = "Run a Sifflet rule",
    description = "Execute a Sifflet data quality rule and get the results"
)
public class RunRule extends Task {
    
    @PluginProperty(dynamic = true)
    @NotNull
    private String apiKey;

    @PluginProperty(dynamic = true)
    @NotNull
    private String ruleId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        String apiUrl = "https://api.siffletdata.com/v1/rules/" + ruleId + "/execute";
        
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(apiUrl);
            
            // Set headers
            request.setHeader("Authorization", "Bearer " + apiKey);
            request.setHeader("Content-Type", "application/json");
            
            // Set empty body for rule execution
            request.setEntity(new StringEntity("{}", ContentType.APPLICATION_JSON));
            
            // Execute request
            return client.execute(request, response -> {
                int statusCode = response.getCode();
                
                if (statusCode >= 200 && statusCode < 300) {
                    Map<String, Object> outputs = new HashMap<>();
                    outputs.put("status", "success");
                    outputs.put("message", "Rule executed successfully");
                    return Output.of(outputs);
                } else {
                    throw new Exception("Failed to execute rule. Status code: " + statusCode);
                }
            });
        } catch (Exception e) {
            throw new Exception("Failed to execute Sifflet rule: " + e.getMessage(), e);
        }
    }
}
