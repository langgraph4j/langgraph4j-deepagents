package org.bsc.langgraph4j.deepagents;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public interface TavilyApi {

    /**
     * Request object for the Tavily API.
     */
    @JsonClassDescription("Request object for the Tavily API")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Request(

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

            public Request build() {
                return new Request(
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Response(
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
        }

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
        }
    }

    class ImageDeserializer extends JsonDeserializer<List<Response.Image>> {
        @Override
        public List<Response.Image> deserialize(JsonParser jsonParser, DeserializationContext context)
                throws IOException {

            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            List<Response.Image> images = new ArrayList<>();

            if (node.isArray()) {
                for (JsonNode element : node) {
                    // If element is a string, treat it as a URL
                    if (element.isTextual()) {
                        images.add(new Response.Image(element.asText(), null));
                    }
                    // If element is an object, map it to Image
                    else if (element.isObject()) {
                        String url = element.has("url") ? element.get("url").asText() : null;
                        String description = element.has("description") ? element.get("description").asText() : null;
                        images.add(new Response.Image(url, description));
                    }
                }
            }

            return images;
        }
    }

    Response search(Request request);
}
