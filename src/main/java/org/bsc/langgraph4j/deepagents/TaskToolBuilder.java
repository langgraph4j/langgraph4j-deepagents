package org.bsc.langgraph4j.deepagents;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.spring.ai.agent.ReactAgent;
import org.bsc.langgraph4j.spring.ai.tool.SpringAIToolResponseBuilder;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.bsc.langgraph4j.utils.CollectionsUtils.mergeMap;

class TaskToolBuilder {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskToolBuilder.class);

    private List<DeepAgent.SubAgent> subAgents;
    private Map<String, ToolCallback> tools;
    private ChatModel model;

    public TaskToolBuilder subAgents(List<DeepAgent.SubAgent> subAgents ) {
        subAgents = List.copyOf( requireNonNull(subAgents,"subAgents cannot be null") );
        return this;
    }

    public TaskToolBuilder tools( Map<String, ToolCallback> tools  ) {
        tools = Map.copyOf( requireNonNull(tools, "tools cannot be null") );
        return this;
    }

    public TaskToolBuilder model( ChatModel model ) {
        this.model = requireNonNull( model, "model cannot be null" );
        return this;
    }

    record TaskToolArgs(
            @JsonProperty(required = true)
            @JsonPropertyDescription("The task to execute with the selected agent")
            String description,
            @JsonProperty(required = true)
            @JsonPropertyDescription("`Name of the agent to use. Available: ${subagents.map((a) => a.name).join(\", \")}`")
            String subAgentType
    ) {
        TaskToolArgs {
            requireNonNull( description, "description cannot be null");
            requireNonNull( subAgentType, "subAgentType cannot be null");
        }
    }

    public ToolCallback build() throws GraphStateException {

        var allTools = mergeMap( tools,
                Map.ofEntries( Tools.ls(),
                        Tools.readFile(),
                        Tools.writeFile(),
                        Tools.editFile(),
                        Tools.writeTodos()));

        var agentsMap = new HashMap<String, StateGraph<DeepAgent.State>>();

        for( var subAgent : subAgents ) {

            var subAgentTools = new ArrayList<ToolCallback>();

            if( subAgent.tools() != null ) {
                for (var toolName : subAgent.tools()) {

                    var resolvedTool = allTools.get(toolName);

                    if (resolvedTool != null) {
                        subAgentTools.add(resolvedTool);
                    } else {
                        log.warn("Warning: Tool '{}' not found for agent '{}'", toolName, subAgent.name());
                    }
                }
            }
            else {
                subAgentTools.addAll( allTools.values() );
            }

            var reactAgent = ReactAgent.<DeepAgent.State>builder()
                    .chatModel( model )
                    .tools( subAgentTools )
                    .schema( DeepAgent.State.SCHEMA )
                    .defaultSystem( subAgent.prompt() )
                    .build();

            agentsMap.put( subAgent.name(), reactAgent );

        }

        return  FunctionToolCallback.<TaskToolArgs, String>builder( "task", (input, context ) -> {

            var reactAgent = agentsMap.get(input.subAgentType());
            if (reactAgent == null ) {
                return format("Error: Agent '%s' not found. Available agents: %s",
                        input.subAgentType(),
                        agentsMap.keySet().stream().collect(Collectors.joining(", ")));
            }

            // final var state = new DeepAgent.State(context.getContext());

            final var inputArgs = GraphInput.args( (Map.of("messages", UserMessage.builder().text(input.description()).build())));
            final var config = RunnableConfig.builder().build();
            try {

                var output = reactAgent.compile().invokeFinal( inputArgs, config );

                var outputState = output.map( o -> o.state() ).orElseThrow();

                return SpringAIToolResponseBuilder.of(context)
                        .update(Map.of("files", outputState.value("files").orElse(Map.of())))
                        .build( outputState.lastMessage()
                                    .map( msg -> msg.getText() )
                                    .orElse( "Task completed"));
            }
            catch( Throwable ex ) {

                return format("Error executing task '%s' with agent '%s': %s",
                        input.description(), input.subAgentType(), ex.getMessage());
            }
        })
        .inputSchema(format("""
                        {
                          "$schema" : "https://json-schema.org/draft/2020-12/schema",
                          "type" : "object",
                          "properties" : {
                            "description" : {
                              "type" : "string",
                              "description" : "The task to execute with the selected agent"
                            },
                            "subAgentType" : {
                              "type" : "string",
                              "description" : "`Name of the agent to use. Available: %s`"
                            }
                          },
                          "required" : [ "description", "subAgentType" ],
                          "additionalProperties" : false
                        }        
                        """, subAgents.stream().map(DeepAgent.SubAgent::name).collect(Collectors.joining(", "))))
        .description(Prompts.TASK_DESCRIPTION_PREFIX.replace(
                "{other_agents}",
                subAgents.stream()
                        .map( a -> format("- %s: %s", a.name(), a.description()))
                        .collect(Collectors.joining("\n"))) +
                Prompts.TASK_DESCRIPTION_SUFFIX)
        .build();

    }
}
