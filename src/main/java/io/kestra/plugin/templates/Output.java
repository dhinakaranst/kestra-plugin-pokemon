package io.kestra.plugin.templates;

import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
public class Output {
    private Map<String, Object> outputs;

    public static OutputBuilder builder() {
        return new OutputBuilder();
    }

    public static class OutputBuilder {
        private Map<String, Object> outputs = new HashMap<>();

        public OutputBuilder put(String key, Object value) {
            outputs.put(key, value);
            return this;
        }

        public Output build() {
            return new Output(outputs);
        }
    }
}
