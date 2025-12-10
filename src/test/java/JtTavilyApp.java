//DEPS org.bsc.langgraph4j:spring-ai-deepagents:1.0-SNAPSHOT
//DEPS org.bsc.langgraph4j:langgraph4j-javelit:1.7-SNAPSHOT

//SOURCES JtTools.java


import io.javelit.core.Jt;
import io.javelit.core.JtComponent;

public class JtTavilyApp {

    public static void main(String[] args) {

        var app = new JtTavilyApp();

        app.view();
    }

    void view() {
        Jt.title("LangGraph4J Deep Agents").use();
        Jt.markdown("### Powered by LangGraph4j and SpringAI").use();

        var tavilyApiKey = Jt.textInput("TAVILY API KEY:")
                .type("password")
                .use();

        if( tavilyApiKey.isBlank() ) {
            Jt.error("TAVILY API KEY cannot be null").use();
            return;
        }

        var tools = new JtTools(tavilyApiKey);

        var query = Jt.textArea("user message:")
                .placeholder("search query")
                .labelVisibility(JtComponent.LabelVisibility.HIDDEN)
                .use();

        if( query.isBlank() ) {
            Jt.error("Query cannot be null").use();
            return;
        }

        var searchResult =  tools.search( query );

        var outputComponent = Jt.expander("Search result").use();
        searchResult.results().forEach( result -> {
            Jt.info( """
            **tile**
            %s
            
            **url**
            %s
            
            **score**
            %s
            
            **content**
            %s
            """.formatted(
                    result.title(),
                    result.url(),
                    result.score(),
                    result.content())).use(outputComponent);
        });
    }

}
