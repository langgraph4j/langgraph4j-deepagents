package org.bsc.langgraph4j.deepagents;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;

/**
 * Client to interact with the Tavily API using Java's HttpClient.
 */
public class TavilyApiHttpClient implements TavilyApi {

    public static class Builder {
        private String tavilyApiKey;

        public Builder tavilyApiKey(String tavilyApiKey) {
            this.tavilyApiKey = tavilyApiKey;
            return this;
        }

        public TavilyApiHttpClient build() {
            return new TavilyApiHttpClient(this);
        }
    };

    public static Builder builder() {
        return new Builder();
    }

    private final String tavilyApiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private TavilyApiHttpClient(Builder builder ) {
        this.httpClient = HttpClient.newHttpClient();
        this.tavilyApiKey = builder.tavilyApiKey;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Response search(Request request) {

        if (request.query() == null || request.query().isEmpty()) {
            throw new IllegalArgumentException("Query parameter is required.");
        }
        DeepAgent.log.info("Received TavilyRequest: {}", request);

        // Build the request payload with all parameters, setting defaults where necessary
        Request requestWithApiKey = Request.builder()
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
                //.apiKey(tavilyApiKey)
                .build();

        DeepAgent.log.debug("Sending request to Tavily API: query={}, searchDepth={}, topic={}, days={}, maxResults={}",
                requestWithApiKey.query(),
                requestWithApiKey.searchDepth(),
                requestWithApiKey.topic(),
                requestWithApiKey.days(),
                requestWithApiKey.maxResults());

        try {
            String requestBody = objectMapper.writeValueAsString(requestWithApiKey);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.tavily.com/search"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer %s".formatted(tavilyApiKey) )
                    .POST( HttpRequest.BodyPublishers.ofString(requestBody) )
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                DeepAgent.log.error("API Error: Status Code {}, Response Body: {}", response.statusCode(), response.body());
                throw new RuntimeException("API Error: " + response.body());
            }

            Response tavilyResponse = objectMapper.readValue(response.body(), Response.class);

            DeepAgent.log.info("Received response from Tavily API for query: {}", requestWithApiKey.query());
            return tavilyResponse;
        } catch (IOException | InterruptedException e) {
            DeepAgent.log.error("HttpClient Error: {}", e.getMessage());
            throw new RuntimeException("HttpClient Error: " + e.getMessage(), e);
        }
    }

}
