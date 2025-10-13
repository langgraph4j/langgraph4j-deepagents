package org.bsc.langgraph4j.deepagents;

import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Reducer;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.bsc.langgraph4j.utils.CollectionsUtils.mergeMap;

class FileChannel implements Channel<Map<String,String>>  {

    private final Reducer<Map<String,String>> reducer;

    @Override
    public Optional<Reducer<Map<String, String>>> getReducer() {
        return Optional.of(reducer);
    }

    @Override
    public Optional<Supplier<Map<String, String>>> getDefault() {
        return Optional.of(Map::of);
    }

    public FileChannel() {
        reducer = this::reduce;
    }

    private Map<String,String> reduce( Map<String,String> map1, Map<String,String> map2 ) {
        return mergeMap( map1, map2, (v1,v2) -> v2 );
    }
}
