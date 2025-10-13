package org.bsc.langgraph4j.deepagents;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bsc.langgraph4j.spring.ai.agentexecutor.AgentExecutor;
import org.bsc.langgraph4j.state.Channel;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.bsc.langgraph4j.utils.CollectionsUtils.mergeMap;

public interface DeepAgent {

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

    }

    @JsonClassDescription("List of todo items to update")
    public record ToDo(
            @JsonProperty(required = true)
            String content,
            @JsonProperty(required = true)
            Status status
    ) {
        public enum Status {
            PENDING,
            IN_PROGRESS,
            COMPLETED
        }
    }

}
