import io.javelit.core.Jt;
import io.javelit.core.JtComponent;
import org.bsc.langgraph4j.deepagents.AiModel;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface JtSelectModel {

    record Model( Provider provider, String name ) {
        enum Provider {
            OLLAMA,
            OPENAI,
            GITHUB,
            VERTEX
        }

        public String apiKeyPlaceholder() {
            return switch( provider ) {
                case OPENAI -> "OpenAI Api Key";
                case OLLAMA -> "";
                case VERTEX -> "Google Cloud Project Id";
                case GITHUB -> "Github Model Token";
            };
        }

        @Override
        public String toString() {
            return "%s:%s".formatted( provider.name().toLowerCase(), name );
        }
    }

    static Optional<ChatModel> get() {

        var selectModelCols = Jt.columns(2).key("select-model-cols").use();

        boolean cloud = Jt.toggle("Select Cloud/Local Model").use(selectModelCols.col(0));
        Jt.markdown(cloud ? "*Cloud*" : "*Local*").use(selectModelCols.col(1));

        try {
            if (cloud) {
                var cloudModelCols = Jt.columns(2).key("cloud-model-cols").use();
                var model = Jt.radio("Available models",
                                List.of(
                                        new Model(Model.Provider.OPENAI, "gpt-4o-mini"),
                                        new Model(Model.Provider.GITHUB,"gpt-4o-mini"),
                                        new Model(Model.Provider.VERTEX,"gemini-2.5-pro")
                                ))
                        .use(cloudModelCols.col(0));

                var apikey = Jt.textInput("API KEY:")
                        .type("password")
                        .labelVisibility(JtComponent.LabelVisibility.HIDDEN)
                        .placeholder( model.apiKeyPlaceholder() )
                        .width(500)
                        .use(cloudModelCols.col(1));
                if (apikey.isEmpty()) {
                    Jt.error("%s cannot be null".formatted( model.apiKeyPlaceholder() )).use();
                    return Optional.empty();
                }
                if( model.provider()  == Model.Provider.VERTEX  ) {
                    var gcpLocation = Jt.textInput("Google Cloud Location:")
                            .labelVisibility(JtComponent.LabelVisibility.HIDDEN)
                            .placeholder( "Google Cloud Location" )
                            .use(cloudModelCols.col(1));
                    if( gcpLocation.isEmpty() ) {
                        Jt.error("Google Cloud Location cannot be null").use();
                        return Optional.empty();
                    }
                    return Optional.of(AiModel.GEMINI.chatModel(model.name(),
                            Map.of("GOOGLE_CLOUD_PROJECT", apikey,
                                    "GOOGLE_CLOUD_LOCATION", gcpLocation)));

                }
                if( model.provider() == Model.Provider.GITHUB  ) {
                    return Optional.of(AiModel.GITHUB_MODEL.chatModel(model.name(), Map.of("GITHUB_MODELS_TOKEN", apikey)));
                }
                return Optional.of(AiModel.OPENAI.chatModel(model.name(), Map.of("OPENAI_API_KEY", apikey)));
            } else {
                var model = Jt.radio("Available models", List.of(
                                new Model(Model.Provider.OLLAMA,"qwen2.5:7b"),
                                new Model(Model.Provider.OLLAMA,"qwen3:8b"),
                                new Model(Model.Provider.OLLAMA,"gpt-oss:20b")
                        ))
                        .use();
                return Optional.of(AiModel.OLLAMA.chatModel(model.name()));
            }
        }
        catch( Throwable ex ) {
            Jt.error(ex.getMessage()).use();
            return Optional.empty();
        }

    }

}
