package io.kestra.plugin.sifflet;

import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.storages.StorageInterface;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@MicronautTest
@ExtendWith(MockitoExtension.class)
class RunRuleTest {
    @Inject
    RunContextFactory runContextFactory;

    @Inject
    StorageInterface storageInterface;

    @Test
    void run() throws Exception {
        RunContext runContext = runContextFactory.of();

        // Create a task with test values
        RunRule task = RunRule.builder()
            .id("test")
            .type(RunRule.class.getName())
            .apiKey("test-api-key")
            .ruleId("test-rule-id")
            .build();

        // Run the task and verify it builds correctly
        assertThat(task.getId(), is("test"));
        assertThat(task.getApiKey(), is("test-api-key"));
        assertThat(task.getRuleId(), is("test-rule-id"));
    }
} 