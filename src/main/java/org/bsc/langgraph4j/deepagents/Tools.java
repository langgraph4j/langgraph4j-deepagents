package org.bsc.langgraph4j.deepagents;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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

    static ToolCallback ls() {
        return  FunctionToolCallback.<Void, Collection<String>>builder( "ls", ( noArgs, context ) -> {
            var state = new DeepAgent.State(context.getContext());

            var result = state.files().keySet();

            DeepAgent.log.debug( "tool: 'ls' call: {}", result );

            return result;
        })
        .description("List all files in the mock filesystem")
        .inputType( Void.class )
        .build();
    }

    record writeTodosArgs(
            @JsonProperty(required = true)
            @JsonPropertyDescription("todo list to update")
            List<DeepAgent.ToDo> toDos
    ) {}

    static ToolCallback writeTodos() {

        final var typeRef = new TypeReference<writeTodosArgs>() {};
        final var mapper = new ObjectMapper();

        return FunctionToolCallback.<writeTodosArgs, String>builder( "write_todos", (input, context ) -> {
            try {
                DeepAgent.log.debug( "tool: 'writeTodos' call: {}", input);

                return SpringAIToolResponseBuilder.of(context)
                        .update(Map.of("todos", input.toDos()))
                        .buildAndReturn( format("Updated todo list to %s", mapper.writeValueAsString(input)) );

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        })
        .inputSchema( JsonSchemaGenerator.generateForType(typeRef.getType()) )
        .inputType(typeRef.getType())
        .description(Prompts.WRITE_TODOS_DESCRIPTION)
        .build();

    }


    record ReadFileArgs(
            @JsonProperty(required = true)
            String filePath,
            @JsonProperty(defaultValue="0")
            int offset,
            @JsonProperty(required=true, defaultValue="2000")
            int limit) {}

    static ToolCallback  readFile() {

        final var typeRef = new TypeReference<ReadFileArgs>() {};

        return FunctionToolCallback.<ReadFileArgs, String>builder( "read_file", ( input, context ) -> {
                    DeepAgent.log.debug( "tool: 'read_file' call: {}", input);

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

                    DeepAgent.log.debug( "tool: 'read_file' {}\n{}", input.filePath(), content);

                    // Split content into lines
                    final var lines = content.split("\n");

                    // Apply line offset and limit
                    final int startIdx = input.offset();
                    final int endIdx = Math.min( startIdx + input.limit(), lines.length);

                    // Handle empty file
                    if (startIdx >= endIdx) {
                        return format("Error: illegal range error [%d,%d] reading file '%s'", startIdx, endIdx, input.filePath());
                    }

                    // Handle case where offset is beyond file length
                    if (startIdx >= lines.length) {
                        return format("Error: Line offset %d exceeds file length %d lines)",
                                input.offset(), lines.length);
                    }

                    // Format output with line numbers (cat -n format)
                    final var resultLines = new ArrayList<String>();

                    for (int i = startIdx; i < endIdx; i++) {
                        var lineContent = lines[i];
                        // Truncate lines longer than 2000 characters
                        if (lineContent.length() > 2000) {
                            lineContent = lineContent.substring(0, 2000);
                        }
                        // Line numbers start at 1, so add 1 to the index
                        resultLines.add( format("%6d\t%s", i + 1, lineContent));
                    }

                    return String.join("\n", resultLines);
                })
                .inputSchema( JsonSchemaGenerator.generateForType(typeRef.getType()) )
                .description(Prompts.TOOL_DESCRIPTION)
                .inputType(typeRef.getType())
                .build();
    }

    record WriteFileArgs(
            @JsonProperty(required = true)
            String filePath,
            @JsonProperty(required = true)
            String  content) {}

    static ToolCallback  writeFile() {
        final var typeRef = new TypeReference<WriteFileArgs>() {};

        return FunctionToolCallback.<WriteFileArgs, String>builder( "write_file", ( input, context ) -> {
                DeepAgent.log.debug( "tool: 'write_file' call: {}", input);

                return SpringAIToolResponseBuilder.of( context )
                            .update( Map.of( "files", Map.of( input.filePath(), input.content() )))
                            .buildAndReturn( format("Updated file %s", input.filePath()) );
        })
        .inputSchema( JsonSchemaGenerator.generateForType(typeRef.getType()) )
        .description("Write content to a file in the mock filesystem")
        .inputType(typeRef.getType())
        .build();

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

    static ToolCallback  editFile() {
        final var typeRef = new TypeReference<EditFileArgs>() {};

        return FunctionToolCallback.<EditFileArgs, String>builder( "edit_file", ( input, context ) -> {
                    DeepAgent.log.debug( "tool: 'edit_file' call: {}", input);

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
                            .buildAndReturn( format("`Updated file %s", input.filePath()) );
                })
                .inputSchema( JsonSchemaGenerator.generateForType(typeRef.getType()) )
                .inputType(typeRef.getType())
                .description(EDIT_DESCRIPTION)
                .build();
    }

    List<ToolCallback> BUILTIN =  List.of(
            Tools.ls(),
            Tools.readFile(),
            Tools.writeFile(),
            Tools.editFile(),
            Tools.writeTodos()
    );

}
