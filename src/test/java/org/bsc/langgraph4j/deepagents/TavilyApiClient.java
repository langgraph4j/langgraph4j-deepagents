package org.bsc.langgraph4j.deepagents;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client to interact with the Tavily API using Spring's RestClient.
 */
@Component
public class TavilyApiClient {

    private final RestClient restClient;

    /**
     * Constructs the TavilyApiClient with a RestClient builder.
     *
     * @param restClientBuilder the RestClient builder
     */
    public TavilyApiClient(RestClient.Builder restClientBuilder, @Value("${TAVILY_API_KEY}") String tavilyApiKey) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.tavily.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tavilyApiKey)
                .build();
    }

    /**
     * Executes a search query against the Tavily API.
     *
     * @param request The TavilyRequest containing query parameters.
     * @return TavilyResponse containing the search results.
     */
    public TavilyResponse search(TavilyRequest request) {

        if (request.query() == null || request.query().isEmpty()) {
            throw new IllegalArgumentException("Query parameter is required.");
        }
        DeepAgent.log.info("Received TavilyRequest: {}", request);

        // Build the request payload with all parameters, setting defaults where necessary
        TavilyRequest requestWithApiKey = TavilyRequest.builder()
                .query(request.query())
                .searchDepth(request.searchDepth() != null ? request.searchDepth() : "basic")
                .topic(request.topic() != null ? request.topic() : "general")
                .days(request.days() != null ? request.days() : 300)
                .maxResults(request.maxResults() != 0 ? request.maxResults() : 10)
                .includeImages(request.includeImages())
                .includeImageDescriptions(request.includeImageDescriptions())
                .includeAnswer(request.includeAnswer())
                .includeRawContent(request.includeRawContent())
                .includeDomains(request.includeDomains() != null ? request.includeDomains() : Collections.emptyList())
                .excludeDomains(request.excludeDomains() != null ? request.excludeDomains() : Collections.emptyList())
                .build();

        DeepAgent.log.debug("Sending request to Tavily API: query={}, searchDepth={}, topic={}, days={}, maxResults={}",
                requestWithApiKey.query(),
                requestWithApiKey.searchDepth(),
                requestWithApiKey.topic(),
                requestWithApiKey.days(),
                requestWithApiKey.maxResults());

        try {
            TavilyResponse response = restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/search").build())
                    .body(requestWithApiKey)
                    .retrieve()
                    .body(TavilyResponse.class);

            DeepAgent.log.info("Received response from Tavily API for query: {}", requestWithApiKey.query());
            return response;
        }
        catch (RestClientResponseException e) {
            DeepAgent.log.error("API Error: Status Code {}, Response Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("API Error: " + e.getStatusText(), e);
        }
        catch (RestClientException e) {
            DeepAgent.log.error("RestClient Error: {}", e.getMessage());
            throw new RuntimeException("RestClient Error: " + e.getMessage(), e);
        }
    }

    /**
     * Request object for the Tavily API.
     */
    @JsonClassDescription("Request object for the Tavily API")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TavilyRequest(

            @JsonProperty("query")
            @JsonPropertyDescription("The main search query.")
            String query,

            @JsonProperty("api_key")
            @JsonPropertyDescription("API key for authentication with Tavily.")
            String apiKey,

            @JsonProperty("search_depth")
            @JsonPropertyDescription("The depth of the search. Accepted values: 'basic', 'advanced'. Default is 'basic'.")
            String searchDepth,

            @JsonProperty("topic")
            @JsonPropertyDescription("The category of the search. Accepted values: 'general', 'news'. Default is 'general'.")
            String topic,

            @JsonProperty("days")
            @JsonPropertyDescription("The number of days back from the current date to include in search results. Default is 3. Only applies to 'news' topic.")
            Integer days,

            @JsonProperty("time_range")
            @JsonPropertyDescription("The time range for search results. Accepted values: 'day', 'week', 'month', 'year' or 'd', 'w', 'm', 'y'. Default is none.")
            String timeRange,

            @JsonProperty("max_results")
            @JsonPropertyDescription("The maximum number of search results to return. Default is 5.")
            int maxResults,

            @JsonProperty("include_images")
            @JsonPropertyDescription("Whether to include a list of query-related images in the response. Default is false.")
            boolean includeImages,

            @JsonProperty("include_image_descriptions")
            @JsonPropertyDescription("When 'include_images' is true, adds descriptive text for each image. Default is false.")
            boolean includeImageDescriptions,

            @JsonProperty("include_answer")
            @JsonPropertyDescription("Whether to include a short answer to the query, generated from search results. Default is false.")
            boolean includeAnswer,

            @JsonProperty("include_raw_content")
            @JsonPropertyDescription("Whether to include the cleaned and parsed HTML content of each search result. Default is false.")
            boolean includeRawContent,

            @JsonProperty("include_domains")
            @JsonPropertyDescription("A list of domains to specifically include in search results. Default is an empty list.")
            List<String> includeDomains,

            @JsonProperty("exclude_domains")
            @JsonPropertyDescription("A list of domains to specifically exclude from search results. Default is an empty list.")
            List<String> excludeDomains
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String query;
            private String apiKey;
            private String searchDepth;
            private String topic;
            private Integer days;
            private String timeRange;
            private int maxResults;
            private boolean includeImages;
            private boolean includeImageDescriptions;
            private boolean includeAnswer;
            private boolean includeRawContent;
            private List<String> includeDomains;
            private List<String> excludeDomains;

            public Builder query(String query) {
                this.query = query;
                return this;
            }

            public Builder apiKey(String apiKey) {
                this.apiKey = apiKey;
                return this;
            }

            public Builder searchDepth(String searchDepth) {
                this.searchDepth = searchDepth;
                return this;
            }

            public Builder topic(String topic) {
                this.topic = topic;
                return this;
            }

            public Builder days(Integer days) {
                this.days = days;
                return this;
            }

            public Builder timeRange(String timeRange) {
                this.timeRange = timeRange;
                return this;
            }

            public Builder maxResults(int maxResults) {
                this.maxResults = maxResults;
                return this;
            }

            public Builder includeImages(boolean includeImages) {
                this.includeImages = includeImages;
                return this;
            }

            public Builder includeImageDescriptions(boolean includeImageDescriptions) {
                this.includeImageDescriptions = includeImageDescriptions;
                return this;
            }

            public Builder includeAnswer(boolean includeAnswer) {
                this.includeAnswer = includeAnswer;
                return this;
            }

            public Builder includeRawContent(boolean includeRawContent) {
                this.includeRawContent = includeRawContent;
                return this;
            }

            public Builder includeDomains(List<String> includeDomains) {
                this.includeDomains = includeDomains;
                return this;
            }

            public Builder excludeDomains(List<String> excludeDomains) {
                this.excludeDomains = excludeDomains;
                return this;
            }

            public TavilyRequest build() {
                return new TavilyRequest(
                        query,
                        apiKey,
                        searchDepth,
                        topic,
                        days,
                        timeRange,
                        maxResults,
                        includeImages,
                        includeImageDescriptions,
                        includeAnswer,
                        includeRawContent,
                        includeDomains,
                        excludeDomains
                );
            }
        }
    }

    /**
     * Response object for the Tavily API.
     */
    @JsonClassDescription("Response object for the Tavily API")
    public record TavilyResponse (
        @JsonProperty("query")
        String query,

        @JsonProperty("follow_up_questions")
        List<String> followUpQuestions,

        @JsonProperty("answer")
        String answer,

        @JsonDeserialize(using = ImageDeserializer.class)
        @JsonProperty("images")
        List<Image> images,

        @JsonProperty("results")
        List<Result> results,

        @JsonProperty("response_time")
        float responseTime
    ) {
        public record Image(
                @JsonProperty("url")
                String url,

                @JsonProperty("description")
                String description
        ) {
        };

        public record Result(
                @JsonProperty("title")
                String title,

                @JsonProperty("url")
                String url,

                @JsonProperty("content")
                String content,

                @JsonProperty("raw_content")
                String rawContent,

                @JsonProperty("score")
                float score,

                @JsonProperty("published_date")
                String publishedDate
        ) {
        };
    }
    public static class ImageDeserializer extends JsonDeserializer<List<TavilyResponse.Image>> {
        @Override
        public List<TavilyApiClient.TavilyResponse.Image> deserialize(JsonParser jsonParser, DeserializationContext context)
                throws IOException {

            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            List<TavilyApiClient.TavilyResponse.Image> images = new ArrayList<>();

            if (node.isArray()) {
                for (JsonNode element : node) {
                    // If element is a string, treat it as a URL
                    if (element.isTextual()) {
                        images.add(new TavilyApiClient.TavilyResponse.Image(element.asText(), null));
                    }
                    // If element is an object, map it to Image
                    else if (element.isObject()) {
                        String url = element.has("url") ? element.get("url").asText() : null;
                        String description = element.has("description") ? element.get("description").asText() : null;
                        images.add(new TavilyApiClient.TavilyResponse.Image(url, description));
                    }
                }
            }

            return images;
        }
    }
}
