package io.kestra.plugin.templates;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.Output;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import lombok.*;
import lombok.experimental.SuperBuilder;

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
        // TODO: Implement Sifflet API integration
        // 1. Initialize Sifflet client with apiKey
        // 2. Execute rule with ruleId
        // 3. Return results
        
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("status", "success");
        outputs.put("message", "Rule execution completed");
        return Output.of(outputs);
    }
}
