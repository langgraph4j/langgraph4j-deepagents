package org.bsc.langgraph4j.deepagents;

import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.spring.ai.agent.ReactAgent;
import org.bsc.langgraph4j.spring.ai.serializer.std.SpringAIStateSerializer;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public class GraphBuilder {

    private List<ToolCallback> tools;
    private String instructions;
    private ChatModel chatModel;
    private List<DeepAgent.SubAgent> subAgents;
    private List<String> builtinTools;

    public GraphBuilder subAgents(List<DeepAgent.SubAgent> subAgents ) {
        this.subAgents = List.copyOf( requireNonNull(subAgents,"subAgents cannot be null") );
        return this;
    }

    public GraphBuilder tools( List<ToolCallback> tools  ) {
        this.tools = List.copyOf( requireNonNull(tools, "tools cannot be null") );
        return this;
    }

    public GraphBuilder chatModel(ChatModel model ) {
        this.chatModel = requireNonNull( model, "model cannot be null" );
        return this;

    }

    public GraphBuilder instructions( String instructions ) {
        this.instructions = instructions;
        return this;

    }
    public GraphBuilder builtinTools( List<String> builtinTools ) {
        this.builtinTools = builtinTools;
        return this;
    }

    StateGraph<DeepAgent.State> build() throws GraphStateException {
        if( tools == null ) {
            tools = List.of();
        }

        // Filter built-in tools if builtinTools parameter is provided
        var  selectedBuiltinTools = (builtinTools!=null)
                ? Tools.BUILTIN.stream().filter(tool ->
                        builtinTools.stream()
                                .anyMatch(bt ->  bt.equals( tool.getToolDefinition().name() )))
                        .toList()
                : Tools.BUILTIN;

        // Combine built-in tools with provided tools
        final var allTools = new ArrayList<>( selectedBuiltinTools );
        //allTools.addAll( tools );

        // Create task tool using createTaskTool() if subagents are provided
        if ( subAgents!= null && !subAgents.isEmpty()) {
            // Create tools map for task tool creation
            var toolsMap = Stream.concat(selectedBuiltinTools.stream(), tools.stream() )
                                .collect( Collectors.toUnmodifiableMap(
                                        tool -> tool.getToolDefinition().name(),
                                        tool -> tool));

            var taskTool = new TaskToolBuilder()
                        .model(chatModel)
                        .subAgents( subAgents )
                        .tools( toolsMap )
                        .build();

            allTools.add(taskTool);
        }

        // Combine instructions with base prompt like Python implementation
        var finalInstructions = instructions!=null
                ? instructions.concat( Prompts.BASE_PROMPT )
                : Prompts.BASE_PROMPT;


        return ReactAgent.<DeepAgent.State>builder()
                .stateSerializer( new SpringAIStateSerializer<>( DeepAgent.State::new ))
                .chatModel(chatModel)
                .tools( allTools )
                .schema( DeepAgent.State.SCHEMA )
                .defaultSystem( finalInstructions )
                .build();
    }
}
