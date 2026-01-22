package ai.freeplay.client.thin.resources.recordings;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class ResponseInfo {
    private final boolean isComplete;
    private OpenAIFunctionCall functionCall;
    private int promptTokens;
    private int responseTokens;

    public ResponseInfo(boolean isComplete) {
        this.isComplete = isComplete;
    }

    public ResponseInfo(
            boolean isComplete,
            OpenAIFunctionCall functionCall,
            int promptTokens,
            int responseTokens
    ) {
        this.isComplete = isComplete;
        this.functionCall = functionCall;
        this.promptTokens = promptTokens;
        this.responseTokens = responseTokens;
    }

    public ResponseInfo functionCall(OpenAIFunctionCall functionCall) {
        this.functionCall = functionCall;
        return this;
    }

    public ResponseInfo promptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
        return this;
    }

    public ResponseInfo responseTokens(int responseTokens) {
        this.responseTokens = responseTokens;
        return this;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public OpenAIFunctionCall getFunctionCall() {
        return functionCall;
    }

    public Map<String, String> getFunctionCallMap() {
        if (functionCall == null) {
            return null;
        }
        return functionCall.asMap();
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public int getResponseTokens() {
        return responseTokens;
    }

    public String toString() {
        return "ResponseInfo{" +
                "isComplete=" + isComplete +
                ", functionCall=" + functionCall +
                ", promptTokens=" + promptTokens +
                ", responseTokens=" + responseTokens +
                '}';
    }
}
