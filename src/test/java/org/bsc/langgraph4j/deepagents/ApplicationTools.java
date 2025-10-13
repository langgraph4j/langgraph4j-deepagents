package org.bsc.langgraph4j.deepagents;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApplicationTools {

    final TavilyApiClient tavilyApiClient;

    public ApplicationTools(TavilyApiClient tavilyApiClient) {
        this.tavilyApiClient = tavilyApiClient;
    }

    record InternetSearchArg(
        @JsonPropertyDescription( "search query")
        @JsonProperty( required = true )
        String query,
        @JsonProperty( required = true, value="max_result", defaultValue = "5" )
        int maxResult,
        @JsonProperty( required = true, defaultValue = "general" )
        @JsonPropertyDescription( "search topic it must be one of 'general', 'news' or 'finance'. Default is 'general'" )
        String topic,
        @JsonProperty( value="include_raw_content", defaultValue = "false" )
        boolean includeRawContent
    ){}
     
    public ToolCallback internetSearch() {
        final var typeRef = new TypeReference<InternetSearchArg>() {};

        return FunctionToolCallback.<InternetSearchArg, List<TavilyApiClient.TavilyResponse.Result>>builder( "internet_search", (input, context ) -> {

                var response = tavilyApiClient.search(TavilyApiClient.TavilyRequest.builder()
                        .query( input.query() )
                        .topic( input.topic() )
                        .includeImages(false)
                        .maxResults( input.maxResult() )
                        .includeRawContent( input.includeRawContent() )
                        .includeAnswer(false)
                        .build());

                DeepAgent.log.info( "internetSearch\n{}", response );

                return response.results();
            })
            .inputSchema(JsonSchemaGenerator.generateForType(typeRef.getType()))
            .description("Run a web search")
            .inputType(typeRef.getType())
            .build();
    }


}
