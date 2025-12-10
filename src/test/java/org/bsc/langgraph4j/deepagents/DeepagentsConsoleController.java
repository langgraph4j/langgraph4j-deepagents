package org.bsc.langgraph4j.deepagents;

import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.RunnableConfig;
import org.springframework.ai.chat.messages.AssistantMessage;
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
    private final ToolsComponent tools;

    public DeepagentsConsoleController(ChatModel chatModel, ToolsComponent tools ) {
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
        //runDeepagentsWithSubAgent( console );
        runDeepagentsWithSubAgents( console );

    }

    public void runDeepagentsWithSubAgents( java.io.Console console ) throws Exception {

        var researchSubagent = DeepAgent.SubAgent.builder()
            .name("research-agent")
            .description("""
                Used to research more in depth questions. Only give this researcher one topic at a time.
                Do not pass multiple sub questions to this researcher.
                Instead, you should break down a large topic into the necessary components, and then call multiple research agents in parallel, one for each sub question.
                """)
            .prompt(AiPrompt.RESEARCH_AGENT.value())
            .tools( List.of( "internet_search" ))
            .build();

        var critiqueSubAgent =  DeepAgent.SubAgent.builder()
                .name("critique-agent")
                .description("Used to critique the final report. Give this agent some information about how you want it to critique the report.")
                .prompt(AiPrompt.CRITIQUE_AGENT.value())
                .build();

        var agent = DeepAgent.builder()
                .instructions(AiPrompt.MAIN_AGENT.value())
                .subAgents( List.of( researchSubagent, critiqueSubAgent ))
                .chatModel(chatModel)
                .tools( List.of( tools.internetSearch()) )
                .build()
                .compile( CompileConfig.builder()
                        .recursionLimit(100)
                        .build());

        String[] userMessages = {
                "What is langgraph4j project?",
                """
                I want that you perform a deep research on: "an overview of the LangGraph4j project"
                """
        };

        Map<String,Object> input = Map.of("messages", new UserMessage(userMessages[1]) );
        var runnableConfig = RunnableConfig.builder().build();

        var result = agent.stream(input, runnableConfig );

        var output = result.stream()
                .peek(s -> System.out.println(s.node()) )
                .reduce((a, b) -> b)
                .orElseThrow();

        console.format( """
        ================================
        TODO
        ================================
        """);
        output.state().todos().forEach((value) -> console.format("""
                -----------
                %s
                -----------
                """, value));

        console.format( """
        ================================
        FILES
        ================================
        """);
        output.state().files().forEach((key, value) -> console.format("""
                file: '%s'
                -----------
                %s
                -----------
                """, key, value));

        console.format( """
        ================================
        FINAL RESULT
        ================================
        """);
        console.format( "result: %s\n",
                output.state().lastMessage()
                        .map(AssistantMessage.class::cast)
                        .map(AssistantMessage::getText)
                        .orElseThrow());

    }

    public void runDeepagentsWithSubAgent(java.io.Console console ) throws Exception {

        var researchSubagent = DeepAgent.SubAgent.builder()
                .name("research-deepagent")
                .description("""
                Used to research more in depth questions. Only give this researcher one topic at a time.
                Do not pass multiple sub questions to this researcher.
                Instead, you should break down a large topic into the necessary components, and then call multiple research agents in parallel, one for each sub question.
                """)
                .prompt("""
                You are a dedicated researcher. Your job is to conduct research based on the users questions.
    
                Conduct thorough research and then reply to the user with a detailed answer to their question
    
                only your FINAL answer will be passed on to the user.
                They will have NO knowledge of anything except your final message, so your final report should be your final message!
                """)
                .tools( List.of( "internet_search" ))
                .build();

        var deepAgent =  DeepAgent.builder()
                .subAgents( List.of( researchSubagent ))
                .chatModel(chatModel)
                .tools( List.of( tools.internetSearch()) )
                .build()
                .compile();
        
        var userMessage = """
                What is langgraph4j project?
                """;

        Map<String,Object> input = Map.of("messages", new UserMessage(userMessage) );
        var runnableConfig = RunnableConfig.builder().build();

        var result = deepAgent.stream(input, runnableConfig );

        var output = result.stream()
                .peek(s -> System.out.println(s.node()) )
                .reduce((a, b) -> b)
                .orElseThrow();

        console.format( "result: %s\n",
                output.state().lastMessage()
                        .map(AssistantMessage.class::cast)
                        .map(AssistantMessage::getText)
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