package org.bsc.langgraph4j.deepagents;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.bsc.langgraph4j.spring.ai.agentexecutor.AgentExecutor;
import org.bsc.langgraph4j.state.Channel;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.bsc.langgraph4j.utils.CollectionsUtils.mergeMap;

public interface DeepAgent {
    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DeepAgent.class);

    class State extends AgentExecutor.State {

        static final Map<String, Channel<?>> SCHEMA = mergeMap( AgentExecutor.State.SCHEMA, Map.of("files", new FileChannel()));

        public List<ToDo> todos() {
            return this.<List<ToDo>>value( "todos")
                    .orElseGet( List::of );
        }

        public Map<String,String> files() {
            return this.<Map<String,String>>value( "files")
                    .orElseGet( Map::of );
        }

        public State(Map<String, Object> initData) {
            super(initData);
        }
    }

    record SubAgent(
            String name,
            String description,
            String prompt,
            List<String> tools
    ) {
        public SubAgent {
            requireNonNull( name, "name cannot be null");
            requireNonNull( description, "description cannot be null");
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private String name;
            private String description;
            private String prompt;
            private List<String> tools;

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder prompt(String prompt) {
                this.prompt = prompt;
                return this;
            }

            public Builder tools(List<String> tools) {
                this.tools = tools;
                return this;
            }

            public SubAgent build() {
                return new SubAgent(name, description, prompt, tools);
            }

        }
    }

    record ToDo(
            @JsonProperty(required = true)
            @JsonPropertyDescription("Content of the todo item")
            String content,
            @JsonProperty(required = true)
            @JsonPropertyDescription("Status of the todo")
            Status status
    ) {
        public enum Status {
            PENDING,
            IN_PROGRESS,
            COMPLETED
        }
    }

    static GraphBuilder builder() {
        return new GraphBuilder();
    }

}
