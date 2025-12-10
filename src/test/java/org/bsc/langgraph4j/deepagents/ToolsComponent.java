package org.bsc.langgraph4j.deepagents;


import org.springframework.stereotype.Component;

@Component
public class ToolsComponent implements AiTools {

    final TavilyApi tavilyApiClient;

    public ToolsComponent(TavilyApi tavilyApiClient) {
        this.tavilyApiClient = tavilyApiClient;
    }

    @Override
    public TavilyApi tavilyApiClient() {
        return tavilyApiClient;
    }


}
