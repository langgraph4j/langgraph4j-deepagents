package org.bsc.langgraph4j.deepagents;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bsc.langgraph4j.spring.ai.tool.SpringAIToolResponseBuilder;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static org.bsc.langgraph4j.deepagents.Prompts.EDIT_DESCRIPTION;

interface Tools {


    static Map.Entry<String,ToolCallback> ls() {
        var tool =  FunctionToolCallback.<Void, Collection<String>>builder( "ls", ( noArgs, context ) -> {
            var state = new DeepAgent.State(context.getContext());

            return state.files().keySet();
        })
        .description("List all files in the mock filesystem")
        .build();

        return Map.entry( "ls", tool );
    }

    static Map.Entry<String,ToolCallback> writeTodos() {

        final var typeRef = new TypeReference<List<DeepAgent.ToDo>>() {};
        final var mapper = new ObjectMapper();

        var tool =  FunctionToolCallback.<List<DeepAgent.ToDo>, String>builder( "write_todos", (input, context ) -> {
            try {

                return SpringAIToolResponseBuilder.of(context)
                        .update(Map.of("todos", input))
                        .build( format("Updated todo list to %s", mapper.writeValueAsString(input)) );

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        })
        .inputSchema( JsonSchemaGenerator.generateForType(typeRef.getType()) )
        .description(Prompts.WRITE_TODOS_DESCRIPTION)
        .build();

        return Map.entry( "write_todos", tool );
    }


    record ReadFileArgs(
            @JsonProperty(required = true)
            String filePath,
            @JsonProperty(defaultValue="0")
            int offset,
            @JsonProperty(defaultValue="2000")
            int limit) {}

    static Map.Entry<String,ToolCallback>  readFile() {

        final var typeRef = new TypeReference<ReadFileArgs>() {};

        var tool =  FunctionToolCallback.<ReadFileArgs, String>builder( "read_file", ( input, context ) -> {
                    final var state = new DeepAgent.State(context.getContext());

                    final var mockFilesystem = state.files();

                    if( !mockFilesystem.containsKey( input.filePath() ) ) {
                        return format("Error: File '%s' not found", input.filePath());
                    }

                    // Get file content
                    final var content = mockFilesystem.get(input.filePath());

                    // Handle empty file
                    if (content.isEmpty()) {
                        return "System reminder: File exists but has empty contents";
                    }

                    // Split content into lines
                    final var lines = content.split("\n");

                    // Apply line offset and limit
                    final var startIdx = input.offset();
                    final var endIdx = Math.min( startIdx + input.limit(), lines.length);

                    // Handle case where offset is beyond file length
                    if (startIdx >= lines.length) {
                        return format("Error: Line offset %d exceeds file length %d lines)",
                                input.offset(), lines.length);
                    }

                    // Format output with line numbers (cat -n format)
                    final var resultLines  = new ArrayList<String>();

                    for (int i = startIdx; i < endIdx; i++) {
                        var lineContent = lines[i];

                        // Truncate lines longer than 2000 characters
                        if (lineContent.length() > 2000) {
                            lineContent = lineContent.substring(0, 2000);
                        }

                        // Line numbers start at 1, so add 1 to the index
                        final var lineNumber = i + 1;
                        resultLines.add( format("%6d\t%s", lineNumber, lineContent));
                    }

                    return String.join("\n", resultLines);
                })
                .inputSchema( JsonSchemaGenerator.generateForType(typeRef.getType()) )
                .description(Prompts.TOOL_DESCRIPTION)
                .build();

        return Map.entry( "read_file", tool );
    }

    record WriteFileArgs(
            @JsonProperty(required = true)
            String filePath,
            @JsonProperty(required = true)
            String  content) {}

    static Map.Entry<String,ToolCallback>  writeFile() {
        final var typeRef = new TypeReference<WriteFileArgs>() {};

        var tool = FunctionToolCallback.<WriteFileArgs, String>builder( "write_file", ( input, context ) ->
                    SpringAIToolResponseBuilder.of( context )
                            .update( Map.of( "files", Map.of( input.filePath(), input.content() )))
                            .build( format("Updated file %s", input.filePath())) )
        .inputSchema( JsonSchemaGenerator.generateForType(typeRef.getType()) )
        .description("Write content to a file in the mock filesystem")
        .build();

        return Map.entry( "write_file", tool );
    }

    record EditFileArgs(
            @JsonProperty(required = true)
            String filePath,
            @JsonProperty(required = true)
            String  oldString,
            @JsonProperty(required = true)
            String  newString,
            boolean replaceAll
    ) {}

    static Map.Entry<String,ToolCallback>  editFile() {
        final var typeRef = new TypeReference<EditFileArgs>() {};

        var tool = FunctionToolCallback.<EditFileArgs, String>builder( "edit_file", ( input, context ) -> {

                    final var state = new DeepAgent.State(context.getContext());

                    final var mockFilesystem = state.files();

                    if( !mockFilesystem.containsKey( input.filePath() ) ) {
                        return format("Error: File '%s' not found", input.filePath());
                    }

                    // Get file content
                    final var content = mockFilesystem.get(input.filePath());

                    // Check if old_string exists in the file
                    if (!content.contains( input.oldString())) {
                        return format("Error: String not found in file: '%s'", input.oldString());
                    }

                    final var escapedOldString = Pattern.quote(input.oldString());

                    if (!input.replaceAll()) {
                        // Escape regex special characters

                        // Count occurrences
                        var pattern = Pattern.compile(escapedOldString);
                        var matcher = pattern.matcher(content);

                        int occurrences = 0;
                        while (matcher.find()) {
                            occurrences++;
                        }

                        // Construct message based on occurrences
                        if (occurrences > 1) {
                            return String.format(
                                    "Error: String '%s' appears %d times in file. Use replace_all=True to replace all instances, or provide a more specific string with surrounding context.",
                                    input.oldString(), occurrences
                            );
                        } else if (occurrences == 0) {
                            return String.format("Error: String not found in file: '%s'", input.oldString());
                        }
                    }

                    var newContent = (input.replaceAll() ) ?
                        content.replaceAll( escapedOldString, input.newString()) :
                        content.replaceFirst( escapedOldString, input.newString());


                    return SpringAIToolResponseBuilder.of(context)
                            .update(Map.of("files", Map.of(input.filePath(), newContent)))
                            .build( format("`Updated file %s", input.filePath()) );
                })
                .inputSchema( JsonSchemaGenerator.generateForType(typeRef.getType()) )
                .description(EDIT_DESCRIPTION)
                .build();

        return Map.entry( "edit_file", tool );
    }
}
