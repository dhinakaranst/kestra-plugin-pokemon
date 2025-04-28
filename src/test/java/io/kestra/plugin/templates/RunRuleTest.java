package io.kestra.plugin.templates;

import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.storages.StorageInterface;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
class RunRuleTest {
    @Inject
    RunContextFactory runContextFactory;

    @Inject
    StorageInterface storageInterface;

    @Test
    void testRunRule() throws Exception {
        RunRule task = RunRule.builder()
            .id("test")
            .type(RunRule.class.getName())
            .apiKey("test-api-key")
            .ruleId("test-rule-id")
            .build();

        RunContext runContext = runContextFactory.of();
        RunRule.Output output = task.run(runContext);

        assertThat(output, is(notNullValue()));
        assertThat(output.getStatus(), is("success"));
        assertThat(output.getMessage(), is("Rule executed successfully"));
    }

    @Test
    void testRunRuleWithInvalidApiKey() {
        RunRule task = RunRule.builder()
            .id("test")
            .type(RunRule.class.getName())
            .apiKey("invalid-api-key")
            .ruleId("test-rule-id")
            .build();

        RunContext runContext = runContextFactory.of();
        
        Exception exception = assertThrows(Exception.class, () -> {
            task.run(runContext);
        });
        
        assertThat(exception.getMessage(), is("Failed to execute Sifflet rule: Failed to execute rule. Status code: 401"));
    }
} 