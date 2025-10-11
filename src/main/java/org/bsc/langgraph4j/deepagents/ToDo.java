package org.bsc.langgraph4j.deepagents;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Todo to track.
 */
@JsonClassDescription("List of todo items to update")
public record ToDo(
        @JsonProperty(required = true)
        String content,
        @JsonProperty(required = true)
        Status status
) {
    public enum Status {
        PENDING,
        IN_PROGRESS,
        COMPLETED
    }
}
