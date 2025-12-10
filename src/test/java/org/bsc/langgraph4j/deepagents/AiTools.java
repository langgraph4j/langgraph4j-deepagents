package org.bsc.langgraph4j.deepagents;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;

import java.util.List;
import java.util.stream.Collectors;

public interface AiTools {

    TavilyApi tavilyApiClient();

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

    default ToolCallback internetSearch() {
        final var typeRef = new TypeReference<InternetSearchArg>() {};

        return FunctionToolCallback.<InternetSearchArg, List<TavilyApi.Response.Result>>builder( "internet_search", (input, context ) -> {

                    var response = tavilyApiClient().search(TavilyApi.Request.builder()
                            .query( input.query() )
                            .topic( input.topic() )
                            .includeImages(false)
                            .maxResults( input.maxResult() )
                            .includeRawContent( input.includeRawContent() )
                            .includeAnswer(false)
                            .build());

                    DeepAgent.log.info( "tool: internet_search\n{}", response.results().stream()
                            .map(TavilyApi.Response.Result::title)
                            .collect(Collectors.joining(" ,")) );

                    return response.results();
                })
                .inputSchema(JsonSchemaGenerator.generateForType(typeRef.getType()))
                .description("Run a web search")
                .inputType(typeRef.getType())
                .build();
    }

}
