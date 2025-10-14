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

import static java.lang.String.format;

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
        //runDeepagentsWithSubAgent( console );
        runDeepagentsWithSubAgents( console );

    }

    public void runDeepagentsWithSubAgents(java.io.Console console ) throws Exception {

        var researchSubagent = DeepAgent.SubAgent.builder()
            .name("research-agent")
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

        var critiqueSubAgent =  DeepAgent.SubAgent.builder()
                .name("critique-agent")
                .description("Used to critique the final report. Give this agent some information about how you want it to critique the report.")
                .prompt("""
                        You are a dedicated editor. You are being tasked to critique a report.
                        
                        You can find the report at `final_report.md`.
                        
                        You can find the question/topic for this report at `question.txt`.
                        
                        The user may ask for specific areas to critique the report in. Respond to the user with a detailed critique of the report. Things that could be improved.
                        
                        You can use the search tool to search for information, if that will help you critique the report
                        
                        Do not write to the `final_report.md` yourself.
                        
                        Things to check:
                        - Check that each section is appropriately named
                        - Check that the report is written as you would find in an essay or a textbook - it should be text heavy, do not let it just be a list of bullet points!
                        - Check that the report is comprehensive. If any paragraphs or sections are short, or missing important details, point it out.
                        - Check that the article covers key areas of the industry, ensures overall understanding, and does not omit important parts.
                        - Check that the article deeply analyzes causes, impacts, and trends, providing valuable insights
                        - Check that the article closely follows the research topic and directly answers questions
                        - Check that the article has a clear structure, fluent language, and is easy to understand.
                        """)
                .build();

        var agent = DeepAgent.builder()
                .instructions("""
                You are an expert researcher. Your job is to conduct thorough research, and then write a polished report.
                
                The first thing you should do is to write the original user question to `question.txt` so you have a record of it.
                
                Use the research-agent to conduct deep research. It will respond to your questions/topics with a detailed answer.
                
                When you think you enough information to write a final report, write it to `final_report.md`
                
                You can call the critique-agent to get a critique of the final report. After that (if needed) you can do more research and edit the `final_report.md`
                You can do this however many times you want until are you satisfied with the result.
                
                Only edit the file once at a time (if you call this tool in parallel, there may be conflicts).
                
                Here are instructions for writing the final report:
                
                <report_instructions>
                
                CRITICAL: Make sure the answer is written in the same language as the human messages! If you make a todo plan - you should note in the plan what language the report should be in so you dont forget!
                Note: the language the report should be in is the language the QUESTION is in, not the language/country that the question is ABOUT.
                
                Please create a detailed answer to the overall research brief that:
                1. Is well-organized with proper headings (# for title, ## for sections, ### for subsections)
                2. Includes specific facts and insights from the research
                3. References relevant sources using [Title](URL) format
                4. Provides a balanced, thorough analysis. Be as comprehensive as possible, and include all information that is relevant to the overall research question. People are using you for deep research and will expect detailed, comprehensive answers.
                5. Includes a "Sources" section at the end with all referenced links
                
                You can structure your report in a number of different ways. Here are some examples:
                
                To answer a question that asks you to compare two things, you might structure your report like this:
                1/ intro
                2/ overview of topic A
                3/ overview of topic B
                4/ comparison between A and B
                5/ conclusion
                
                To answer a question that asks you to return a list of things, you might only need a single section which is the entire list.
                1/ list of things or table of things
                Or, you could choose to make each item in the list a separate section in the report. When asked for lists, you don't need an introduction or conclusion.
                1/ item 1
                2/ item 2
                3/ item 3
                
                To answer a question that asks you to summarize a topic, give a report, or give an overview, you might structure your report like this:
                1/ overview of topic
                2/ concept 1
                3/ concept 2
                4/ concept 3
                5/ conclusion
                
                If you think you can answer the question with a single section, you can do that too!
                1/ answer
                
                REMEMBER: Section is a VERY fluid and loose concept. You can structure your report however you think is best, including in ways that are not listed above!
                Make sure that your sections are cohesive, and make sense for the reader.
                
                For each section of the report, do the following:
                - Use simple, clear language
                - Use ## for section title (Markdown format) for each section of the report
                - Do NOT ever refer to yourself as the writer of the report. This should be a professional report without any self-referential language.\s
                - Do not say what you are doing in the report. Just write the report without any commentary from yourself.
                - Each section should be as long as necessary to deeply answer the question with the information you have gathered. It is expected that sections will be fairly long and verbose. You are writing a deep research report, and users will expect a thorough answer.
                - Use bullet points to list out information when appropriate, but by default, write in paragraph form.
                
                REMEMBER:
                The brief and research may be in English, but you need to translate this information to the right language when writing the final answer.
                Make sure the final answer report is in the SAME language as the human messages in the message history.
                
                Format the report in clear markdown with proper structure and include source references where appropriate.
                
                <Citation Rules>
                - Assign each unique URL a single citation number in your text
                - End with ### Sources that lists each source with corresponding numbers
                - IMPORTANT: Number sources sequentially without gaps (1,2,3,4...) in the final list regardless of which sources you choose
                - Each source should be a separate line item in a list, so that in markdown it is rendered as a list.
                - Example format:
                  [1] Source Title: URL
                  [2] Source Title: URL
                - Citations are extremely important. Make sure to include these, and pay a lot of attention to getting these right. Users will often use these citations to look into more information.
                </Citation Rules>
                </report_instructions>
                
                You have access to a few tools.
                
                ## `internet_search`
                
                Use this to run an internet search for a given query. You can specify the number of results, the topic, and whether raw content should be included.
                
                """)
                .subAgents( List.of( researchSubagent, critiqueSubAgent ))
                .chatModel(chatModel)
                .tools( List.of( tools.internetSearch()) )
                .build()
                .compile( CompileConfig.builder()
                        .recursionLimit(100)
                        .build());

        var userMessage = """
                        What is langgraph4j project?
                        """;

        Map<String,Object> input = Map.of("messages", new UserMessage(userMessage) );
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