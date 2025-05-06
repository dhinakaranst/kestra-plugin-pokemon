package io.kestra.plugin.templates;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.storages.StorageInterface;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@MicronautTest
class RunRuleTest {
    private static final Logger logger = LoggerFactory.getLogger(RunRuleTest.class);

    @Inject
    RunContextFactory runContextFactory;

    @Inject
    StorageInterface storageInterface;

    @Inject
    Validator validator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRunRule() throws Exception {
        // For now, we'll just verify that the task is properly configured
        RunRule task = RunRule.builder()
            .id("test")
            .type(RunRule.class.getName())
            .apiKey("test-api-key")
            .ruleId("test-rule-id")
            .build();

        assertThat(task, is(notNullValue()));
        assertThat(task.getApiKey(), is("test-api-key"));
        assertThat(task.getRuleId(), is("test-rule-id"));
    }

    @Test
    void testRunRuleValidation() {
        RunRule task = RunRule.builder()
            .build();

        Set<ConstraintViolation<RunRule>> violations = validator.validate(task);
        
        // Print all violations for debugging
        violations.forEach(violation -> {
            logger.info("Violation: {} - {}", violation.getPropertyPath(), violation.getMessage());
        });

        // We expect 8 violations: null and blank checks for id, type, apiKey, and ruleId
        assertThat(violations, hasSize(8));
        
        // Convert violations to a set of strings in the format "property - message"
        Set<String> violationStrings = violations.stream()
            .map(v -> v.getPropertyPath() + " - " + v.getMessage())
            .collect(java.util.stream.Collectors.toSet());
        
        // Verify all expected violations are present
        assertThat(violationStrings, containsInAnyOrder(
            "id - must not be null",
            "id - must not be blank",
            "type - must not be null",
            "type - must not be blank",
            "apiKey - must not be null",
            "apiKey - must not be blank",
            "ruleId - must not be null",
            "ruleId - must not be blank"
        ));
    }
} 