package org.bsc.langgraph4j.deepagents;

import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.RunnableConfig;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

/**
 * Demonstrates the use of Spring Boot CLI to execute a task using an agent executor.
 */
@Controller
public class DeepagentsConsoleController implements CommandLineRunner {
    private static final org.slf4j.Logger log = DeepAgent.log;

    private final ChatModel chatModel;
    private final ApplicationTools tools;

    public DeepagentsConsoleController(ChatModel chatModel, ApplicationTools tools ) {
        this.chatModel = chatModel;
        this.tools = tools;
    }

    /**
     * Executes the command-line interface to demonstrate a Spring Boot application.
     * This method logs a welcome message, constructs a graph using an agent executor,
     * compiles it into a workflow, invokes the workflow with a specific input,
     * and then logs the final result.
     *
     * @param args Command line arguments (Unused in this context)
     * @throws Exception If any error occurs during execution
     */
    @Override
    public void run(String... args) throws Exception {

        log.info("Welcome to the Spring Boot CLI application!");

        var console = System.console();


        // runDeepagents( console  );
        runDeepagentsWithSubAgent( console );
    }

    public void runDeepagentsWithSubAgent(java.io.Console console ) throws Exception {

        var compileConfig = CompileConfig.builder().build();

        var research_subagent = new DeepAgent.SubAgent(
                "research-agent",
                "Used to research more in depth questions",
                """
                You are an expert researcher. Your job is to conduct thorough research, and then write a polished report.

                You have access to a few tools.

                ## `internet_search`

                Use this to run an internet search for a given query. You can specify the number of results, the topic, and whether raw content should be included.
                """,
                List.of( "internet_search" ));

                var agent =  DeepAgent.builder()
                        .subAgents( List.of( research_subagent ))
                        .chatModel(chatModel)
                        .tools( List.of( tools.internetSearch()) )
                        .build()
                        .compile(compileConfig);
        
                log.info( "{}", agent.getGraph( GraphRepresentation.Type.MERMAID, "ReAct Agent", false));
        
                var userMessage = """
                        what is langgraph?
                        """;

        Map<String,Object> input = Map.of("messages", new UserMessage(userMessage) );
        var runnableConfig = RunnableConfig.builder().build();

        var result = agent.stream(input, runnableConfig );

        var output = result.stream()
                .peek(s -> System.out.println(s.node()) )
                .reduce((a, b) -> b)
                .orElseThrow();

        console.format( "result: %s\n",
                output.state().lastMessage()
                        //.map(AssistantMessage.class::cast)
                        //.map(AssistantMessage::getText)
                        .orElseThrow());

    }

    public void runDeepagents(java.io.Console console ) throws Exception {
        var instructions = """
        "You are an expert researcher. Your job is to conduct thorough research, and then write a polished report.
        
        You have access to a few tools.
        
        ## `internet_search`
        
        Use this to run an internet search for a given query. You can specify the number of results, the topic, and whether raw content should be included.
        """;

        var compileConfig = CompileConfig.builder()
                .build();

        var agent =  DeepAgent.builder()
                .instructions(instructions)
                .chatModel(chatModel)
                .tools( List.of( tools.internetSearch()) )
                .build()
                .compile(compileConfig);

        log.info( "{}", agent.getGraph( GraphRepresentation.Type.MERMAID, "ReAct Agent", false));

        var userMessage = """
        "what is langgraph?"
        """;

        Map<String,Object> input = Map.of("messages", new UserMessage(userMessage) );
        var runnableConfig = RunnableConfig.builder().build();

        var result = agent.stream(input, runnableConfig );

        var output = result.stream()
                .peek(s -> System.out.println(s.node()) )
                .reduce((a, b) -> b)
                .orElseThrow();

        console.format( "result: %s\n",
                output.state().lastMessage()
                        //.map(AssistantMessage.class::cast)
                        //.map(AssistantMessage::getText)
                        .orElseThrow());

    }

}