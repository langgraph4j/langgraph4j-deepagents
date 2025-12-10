//DEPS org.bsc.langgraph4j:spring-ai-deepagents:1.0-SNAPSHOT
//DEPS org.bsc.langgraph4j:langgraph4j-javelit:1.7-SNAPSHOT
//DEPS net.sourceforge.plantuml:plantuml-mit:1.2025.10
//DEPS org.springframework.ai:spring-ai-bom:1.1.0@pom
//DEPS org.springframework.ai:spring-ai-client-chat
//DEPS org.springframework.ai:spring-ai-openai
//DEPS org.springframework.ai:spring-ai-ollama
//DEPS org.springframework.ai:spring-ai-vertex-ai-gemini
//DEPS org.springframework.ai:spring-ai-azure-openai

//SOURCES org/bsc/langgraph4j/deepagents/AiModel.java
//SOURCES org/bsc/langgraph4j/deepagents/AiPrompt.java
//SOURCES JtTools.java
//SOURCES JtSelectModel.java

import io.javelit.core.Jt;
import io.javelit.core.JtComponent;
import org.bsc.javelit.SpinnerComponent;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.deepagents.*;
import org.bsc.langgraph4j.streaming.StreamingOutput;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.bsc.javelit.JtPlantUMLImage;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class JtDeepAgentsApp {

    public static void main(String[] args) {

        var app = new JtDeepAgentsApp();

        app.view();
    }

    void view() {
        Jt.title("LangGraph4J Deep Agents").use();
        Jt.markdown("### Powered by LangGraph4j and SpringAI").use();

        var chatModel = JtSelectModel.get();

        if( chatModel.isEmpty() ) return;

        var tavilyApiKey = Jt.textInput("TAVILY API KEY:")
                .type("password")
                .use();

        if( tavilyApiKey.isBlank() ) {
            Jt.error("TAVILY API KEY cannot be null").use();
            return;
        }

        var agent = buildAgent( chatModel.get(), new JtTools(tavilyApiKey)  );

        if( agent.isEmpty() ) return;

        if (Jt.toggle("Show PlantUML Diagram").value(false).use()) {
            JtPlantUMLImage.build(agent.get().getGraph(GraphRepresentation.Type.PLANTUML,
                            "Deep Agents",
                            false))
                    .ifPresent(cb -> {
                        cb.use();
                        Jt.divider("plantuml-divider").use();
                    });
        }

        var userMessage = Jt.textArea("user message:")
                .placeholder("user message")
                .labelVisibility(JtComponent.LabelVisibility.HIDDEN)
                .use();

        var start = Jt.button("start agent")
                .disabled(userMessage.isBlank())
                .use();

        if( start ) {
            executeAgent( agent.get(), userMessage );
        }

    }

    

    Optional<CompiledGraph<DeepAgent.State>> buildAgent( ChatModel chatModel, JtTools tools )  {

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

        try {
            var agent = DeepAgent.builder()
                    .instructions(AiPrompt.MAIN_AGENT.value())
                    .subAgents(List.of(researchSubagent, critiqueSubAgent))
                    .chatModel(chatModel)
                    .tools(List.of(tools.internetSearch()))
                    .build()
                    .compile(CompileConfig.builder()
                            .recursionLimit(100)
                            .build());
            return Optional.of(agent);
        }
        catch( Exception ex  ) {
            Jt.error(ex.getMessage()).use();
            return Optional.empty();
        }
    }

    void executeAgent( CompiledGraph<DeepAgent.State> agent, String userMessage ) {

            var spinner = SpinnerComponent.builder()
                    .message("**starting the agent** ....")
                    .showTime(true)
                    .use();

            var outputComponent = Jt.expander("Workflow Steps").use();

            var input = Map.<String, Object>of("messages", new UserMessage(userMessage));

            var runnableConfig = RunnableConfig.builder()
                    .threadId("deepagents-01")
                    .build();

            var generator = agent.stream(input, runnableConfig);


            try {

                final var startTime = Instant.now();

                var output = generator.stream()
                        .peek(s -> {
                            if (s instanceof StreamingOutput<?> out) {
                                var prev = Jt.sessionState().getString("streaming", "");

                                if (!out.chunk().isEmpty()) {

                                    var partial = prev + out.chunk();

                                    Jt.markdown("""
                                                #### %s
                                                ```
                                                %s
                                                ```
                                                ***
                                                """.formatted(out.node(), partial)).use(outputComponent);

                                    Jt.sessionState().put("streaming", partial);
                                }
                            } else {

                                Jt.sessionState().remove("streaming");
                                Jt.info("""
                                            #### %s
                                            ```
                                            %s
                                            ```
                                            """.formatted(s.node(),
                                        s.state().messages().stream()
                                                .map(Object::toString)
                                                .collect(Collectors.joining("\n\n")))
                                ).use(outputComponent);
                            }
                        })
                        .reduce((a, b) -> b)
                        .orElseThrow();


                var response = output.state().lastMessage()
                        .map(Object::toString)
                        .orElse("No response found");

                final var elapsedTime = Duration.between(startTime, Instant.now());

                Jt.success("finished in %ds%n%n%s".formatted(elapsedTime.toSeconds(), response))
                        .use(spinner);
            } catch (Exception e) {
                Jt.error(e.getMessage()).use(spinner);
            }

        }
}
