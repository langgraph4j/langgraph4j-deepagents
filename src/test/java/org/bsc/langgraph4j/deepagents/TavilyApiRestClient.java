package org.bsc.langgraph4j.deepagents;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collections;

/**
 * Client to interact with the Tavily API using Spring's RestClient.
 */
@Component
public class TavilyApiRestClient implements TavilyApi {

    private final RestClient restClient;

    /**
     * Constructs the TavilyApiClient with a RestClient builder.
     *
     * @param restClientBuilder the RestClient builder
     */
    public TavilyApiRestClient(RestClient.Builder restClientBuilder, @Value("${TAVILY_API_KEY}") String tavilyApiKey) {
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
                .build();

        DeepAgent.log.debug("Sending request to Tavily API: query={}, searchDepth={}, topic={}, days={}, maxResults={}",
                requestWithApiKey.query(),
                requestWithApiKey.searchDepth(),
                requestWithApiKey.topic(),
                requestWithApiKey.days(),
                requestWithApiKey.maxResults());

        try {
            Response response = restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/search").build())
                    .body(requestWithApiKey)
                    .retrieve()
                    .body(Response.class);

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

}
