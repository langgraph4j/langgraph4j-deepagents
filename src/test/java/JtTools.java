import org.bsc.langgraph4j.deepagents.AiTools;
import org.bsc.langgraph4j.deepagents.TavilyApi;
import org.bsc.langgraph4j.deepagents.TavilyApiHttpClient;

public class JtTools implements AiTools {

    private final TavilyApiHttpClient service;

    public JtTools( String tavilyApiKey ) {

        service = TavilyApiHttpClient.builder().tavilyApiKey(tavilyApiKey).build();

    }

    public TavilyApi.Response search( String query ) {
        return tavilyApiClient().search( TavilyApi.Request.builder()
                .query( query )
                .topic( "general" )
                .includeImages(false)
                .maxResults( 5 )
                .includeRawContent( false )
                .includeAnswer(false)
                .build() );
    }

    @Override
    public TavilyApi tavilyApiClient() {
        return service;
    }
}
