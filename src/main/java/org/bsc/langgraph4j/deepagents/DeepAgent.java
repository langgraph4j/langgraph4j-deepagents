package org.bsc.langgraph4j.deepagents;

import org.bsc.langgraph4j.spring.ai.agentexecutor.AgentExecutor;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public interface DeepAgent {

    class State extends AgentExecutor.State {

        static final Map<String, Channel<?>> SCHEMA = Map.of("files", new FileChannel());

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

}
