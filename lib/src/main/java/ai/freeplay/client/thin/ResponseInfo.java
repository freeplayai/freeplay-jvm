package ai.freeplay.client.thin;

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

    public int getPromptTokens() {
        return promptTokens;
    }

    public int getResponseTokens() {
        return responseTokens;
    }
}
