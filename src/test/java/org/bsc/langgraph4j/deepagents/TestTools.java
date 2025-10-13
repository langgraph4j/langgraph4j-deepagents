package org.bsc.langgraph4j.deepagents;

import com.fasterxml.jackson.core.type.TypeReference;
import org.bsc.langgraph4j.utils.TypeRef;
import org.junit.jupiter.api.Test;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestTools {


    @Test
    public void testJsonSchema() {
        var listTodoTypeRef = new TypeReference<List<DeepAgent.ToDo>>() {};

        var listToDoSchema = JsonSchemaGenerator.generateForType(listTodoTypeRef.getType());

        assertNotNull( listToDoSchema, "listToDoSchema is null" );

        var taskToolTypeRef = new TypeReference<TaskToolBuilder.TaskToolArgs>() {};

        var taskToolSchema = JsonSchemaGenerator.generateForType(taskToolTypeRef.getType());

        assertNotNull( taskToolSchema, "taskToolSchema is null" );

        System.out.println( JsonSchemaGenerator.generateForType(String.class) );
    }

    @Test
    public void testFileChannel() {

        final var fileChannel = new FileChannel();

        final var typeRef = new TypeRef<Map<String,String>>() {};

        var result = fileChannel.update( "key1", Map.of(), Map.of( "key2", "value2"));

        assertTrue( typeRef.cast(result).isPresent() );

        assertEquals(  Map.of( "key2", "value2"), result );

        result = fileChannel.update( "key1", result , Map.of( "key3", "value3", "key4", "value4"));

        assertEquals(  Map.of( "key2", "value2", "key3", "value3", "key4", "value4"),
                        result) ;

        result = fileChannel.update( "key1", result , Map.of( "key3", "value31", "key4", "value4"));
        assertEquals(  Map.of( "key2", "value2", "key3", "value31", "key4", "value4"),
                result );
    }
}
