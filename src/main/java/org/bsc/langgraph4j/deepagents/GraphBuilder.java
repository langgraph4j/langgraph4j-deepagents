package org.bsc.langgraph4j.deepagents;

import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.spring.ai.agent.ReactAgent;
import org.bsc.langgraph4j.spring.ai.serializer.std.SpringAIStateSerializer;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class GraphBuilder {

/**
 * Base prompt that provides instructions about available tools
 * Ported from Python implementation to ensure consistent behavior
 */
static final String BASE_PROMPT = """
You have access to a number of standard tools

## `write_todos`

You have access to the `write_todos` tools to help you manage and plan tasks. Use these tools VERY frequently to ensure that you are tracking your tasks and giving the user visibility into your progress.
These tools are also EXTREMELY helpful for planning tasks, and for breaking down larger complex tasks into smaller steps. If you do not use this tool when planning, you may forget to do important tasks - and that is unacceptable.

It is critical that you mark todos as completed as soon as you are done with a task. Do not batch up multiple tasks before marking them as completed.

## `task`

- When doing web search, prefer to use the `task` tool in order to reduce context usage.`;
""";

    /**
     * Built-in tools that are always available in Deep Agents
     */
    static final List<ToolCallback> BUILTIN_TOOLS = List.of(
            Tools.ls().getValue(),
            Tools.readFile().getValue(),
            Tools.writeFile().getValue(),
            Tools.editFile().getValue(),
            Tools.writeTodos().getValue()
    );

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

        // Filter built-in tools if builtinTools parameter is provided
        var  selectedBuiltinTools = (builtinTools!=null)
                ? BUILTIN_TOOLS.stream().filter(tool ->
                        builtinTools.stream()
                                .anyMatch(bt ->  bt.equals( tool.getToolDefinition().name() )))
                        .toList()
                : BUILTIN_TOOLS;

        // Combine built-in tools with provided tools
        var allTools = new ArrayList<>( selectedBuiltinTools );
        allTools.addAll( tools );

        // Create task tool using createTaskTool() if subagents are provided
        if ( subAgents!= null && !subAgents.isEmpty()) {
            // Create tools map for task tool creation
            var toolsMap = allTools.stream().collect( Collectors.toUnmodifiableMap( tool -> tool.getToolDefinition().name(), tool -> tool));

            var taskTool = new TaskToolBuilder()
                        .model(chatModel)
                        .subAgents( subAgents )
                        .tools( toolsMap )
                        .build();

            allTools.add(taskTool);
        }

        // Combine instructions with base prompt like Python implementation
        var finalInstructions = instructions!=null
                ? instructions.concat( BASE_PROMPT )
                : BASE_PROMPT;


        return ReactAgent.<DeepAgent.State>builder()
                .stateSerializer( new SpringAIStateSerializer<>( DeepAgent.State::new ))
                .chatModel(chatModel)
                .tools( allTools )
                .schema( DeepAgent.State.SCHEMA )
                .defaultSystem( finalInstructions )
                .build();
    }
}
