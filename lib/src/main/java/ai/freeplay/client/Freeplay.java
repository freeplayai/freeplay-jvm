package ai.freeplay.client;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.flavor.ChatFlavor;
import ai.freeplay.client.internal.CallSupport;
import ai.freeplay.client.internal.ParameterUtils;
import ai.freeplay.client.model.*;
import ai.freeplay.client.processor.APITemplateResolver;
import ai.freeplay.client.processor.ChatPromptProcessor;
import ai.freeplay.client.processor.TemplateResolver;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class Freeplay {
    private final CallSupport callSupport;

    public Freeplay(String freeplayAPIKey, String baseUrl, ProviderConfigs providerConfigs) {
        this(freeplayAPIKey, baseUrl, providerConfigs, null, new HttpConfig());
    }

    public Freeplay(
            String freeplayApiKey,
            String baseUrl,
            ProviderConfigs providerConfigs,
            RecordProcessor recordProcessor
    ) {
        this(freeplayApiKey, baseUrl, providerConfigs, null, null, new HttpConfig(), recordProcessor);
    }

    public Freeplay(
            String freeplayAPIKey,
            String baseUrl,
            ProviderConfigs providerConfigs,
            Map<String, Object> llmParameters
    ) {
        this(freeplayAPIKey, baseUrl, providerConfigs, null, llmParameters, new HttpConfig());
    }

    public Freeplay(
            String freeplayAPIKey,
            String baseUrl,
            ProviderConfigs providerConfigs,
            Map<String, Object> llmParameters,
            HttpConfig httpConfig
    ) {
        this(freeplayAPIKey, baseUrl, providerConfigs, null, llmParameters, httpConfig);
    }

    public Freeplay(
            String freeplayAPIKey,
            String baseUrl,
            ProviderConfigs providerConfigs,
            Map<String, Object> llmParameters,
            HttpConfig httpConfig,
            RecordProcessor recordProcessor
    ) {
        this(freeplayAPIKey, baseUrl, providerConfigs, null, llmParameters, httpConfig, recordProcessor);
    }

    public Freeplay(
            String freeplayAPIKey,
            String baseUrl,
            ProviderConfigs providerConfigs,
            ChatFlavor flavor,
            Map<String, Object> llmParameters,
            HttpConfig httpConfig
    ) {
        this(freeplayAPIKey, baseUrl, providerConfigs, flavor, llmParameters, httpConfig, null);
    }

    public Freeplay(
            String freeplayAPIKey,
            String baseUrl,
            ProviderConfigs providerConfigs,
            ChatFlavor flavor,
            Map<String, Object> llmParameters,
            HttpConfig httpConfig,
            RecordProcessor recordProcessor
    ) {
        callSupport = new CallSupport(
                freeplayAPIKey,
                baseUrl,
                providerConfigs,
                flavor,
                llmParameters,
                httpConfig,
                recordProcessor,
                new APITemplateResolver(baseUrl, freeplayAPIKey, httpConfig)
        );
    }

    public Freeplay(FreeplayConfig freeplayConfig) {
        freeplayConfig.validate();
        callSupport = new CallSupport(
                freeplayConfig.freeplayAPIKey,
                freeplayConfig.baseUrl,
                freeplayConfig.providerConfigs,
                null,
                freeplayConfig.llmParameters,
                freeplayConfig.httpConfig,
                freeplayConfig.recordProcessor,
                freeplayConfig.templateResolver
        );
    }

    // ====================================================
    // Backward compatible constructors
    // ====================================================

    @Deprecated
    public Freeplay(String freeplayAPIKey, String baseUrl, ProviderConfig providerConfig) {
        this(freeplayAPIKey, baseUrl, providerConfig, null, null, new HttpConfig());
    }

    @Deprecated
    public Freeplay(
            String freeplayAPIKey,
            String baseUrl,
            ProviderConfig providerConfig,
            Map<String, Object> llmParameters
    ) {
        this(freeplayAPIKey, baseUrl, providerConfig, null, llmParameters, new HttpConfig());
    }

    @Deprecated
    public Freeplay(
            String freeplayAPIKey,
            String baseUrl,
            ProviderConfig providerConfig,
            Map<String, Object> llmParameters,
            HttpConfig httpConfig
    ) {
        this(freeplayAPIKey, baseUrl, providerConfig, null, llmParameters, httpConfig);
    }

    @Deprecated
    public Freeplay(
            String freeplayAPIKey,
            String baseUrl,
            ProviderConfig providerConfig,
            ChatFlavor flavor,
            Map<String, Object> llmParameters,
            HttpConfig httpConfig
    ) {
        this(freeplayAPIKey,
                baseUrl,
                ProviderConfigs.fromGenericConfig(providerConfig),
                flavor,
                llmParameters,
                httpConfig
        );
    }

    public CompletionSession createSession(String projectId, String environment) {
        return createSession(projectId, environment, Collections.emptyMap());
    }

    public CompletionSession createSession(String projectId, String environment, Map<String, Object> metadata) {
        String sessionId = CallSupport.createSessionId();
        ParameterUtils.validateBasicMap(metadata);
        Collection<PromptTemplate> prompts = callSupport.getPrompts(projectId, environment);

        return new CompletionSession(callSupport, sessionId, prompts, environment, null, metadata);
    }

    public CompletionResponse getCompletion(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            String environment
    ) throws FreeplayException {
        return getCompletion(projectId, templateName, variables, Collections.emptyMap(), environment);
    }

    public CompletionResponse getCompletion(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment
    ) throws FreeplayException {
        return getCompletion(projectId, templateName, variables, llmParameters, environment, null, null);
    }

    public CompletionResponse getCompletion(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment,
            ChatFlavor flavor
    ) throws FreeplayException {
        return getCompletion(
                projectId,
                templateName,
                variables,
                llmParameters,
                environment,
                flavor,
                null
        );
    }

    public CompletionResponse getCompletion(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment,
            ChatPromptProcessor promptProcessor
    ) throws FreeplayException {
        return getCompletion(
                projectId,
                templateName,
                variables,
                llmParameters,
                environment,
                null,
                promptProcessor
        );
    }

    public CompletionResponse getCompletion(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment,
            ChatFlavor flavor,
            ChatPromptProcessor promptProcessor
    ) throws FreeplayException {
        return getCompletion(
                projectId,
                templateName,
                variables,
                llmParameters,
                environment,
                flavor,
                promptProcessor,
                Collections.emptyMap());
    }

    public CompletionResponse getCompletion(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment,
            ChatFlavor flavor,
            ChatPromptProcessor promptProcessor,
            Map<String, Object> metadata
    ) throws FreeplayException {
        String sessionId = CallSupport.createSessionId();
        ParameterUtils.validateBasicMap(metadata);
        Collection<PromptTemplate> prompts = callSupport.getPrompts(projectId, environment);
        return callSupport.prepareAndMakeCall(
                sessionId,
                prompts,
                templateName,
                variables,
                llmParameters,
                metadata,
                environment,
                null,   // testRunId
                flavor,
                promptProcessor
        );
    }

    public ChatStart<IndexedChatMessage> startChat(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            String environment
    ) throws FreeplayException {
        return startChat(projectId, templateName, variables, Collections.emptyMap(), environment);
    }

    public ChatStart<IndexedChatMessage> startChat(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment
    ) throws FreeplayException {
        return startChat(projectId, templateName, variables, llmParameters, environment, null);
    }

    public ChatStart<IndexedChatMessage> startChat(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment,
            ChatFlavor flavor
    ) throws FreeplayException {
        return startChat(projectId, templateName, variables, llmParameters, environment, flavor, Collections.emptyMap());
    }


    public ChatStart<IndexedChatMessage> startChat(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment,
            ChatFlavor flavor,
            Map<String, Object> metadata
    ) throws FreeplayException {
        String sessionId = CallSupport.createSessionId();
        ParameterUtils.validateBasicMap(metadata);
        Collection<PromptTemplate> prompts = callSupport.getPrompts(projectId, environment);
        ChatSession chatSession =
                new ChatSession(
                        callSupport,
                        sessionId,
                        prompts,
                        templateName,
                        environment,
                        metadata);
        return chatSession.startChat(
                variables,
                llmParameters,
                environment,
                flavor
        );
    }

    public ChatStart<Stream<IndexedChatMessage>> startChatStream(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment
    ) throws FreeplayException {
        return startChatStream(projectId, templateName, variables, llmParameters, environment, null);
    }

    public ChatStart<Stream<IndexedChatMessage>> startChatStream(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment,
            ChatFlavor flavor
    ) throws FreeplayException {
        return startChatStream(projectId, templateName, variables, llmParameters, environment, flavor, Collections.emptyMap());
    }

    public ChatStart<Stream<IndexedChatMessage>> startChatStream(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment,
            ChatFlavor flavor,
            Map<String, Object> metadata
    ) throws FreeplayException {
        String sessionId = CallSupport.createSessionId();
        ParameterUtils.validateBasicMap(metadata);
        Collection<PromptTemplate> prompts = callSupport.getPrompts(projectId, environment);
        ChatSession chatSession = new ChatSession(callSupport, sessionId, prompts, templateName, environment, metadata);
        return new ChatStart<>(
                chatSession,
                chatSession.startChatStream(
                        variables,
                        llmParameters,
                        environment,
                        flavor
                ));
    }

    public TestRun createTestRun(String projectId, String environment, String testListName) {
        return callSupport.createTestRun(projectId, environment, testListName);
    }

    public void recordCompletionFeedback(String projectId, String completionId, Map<String, Object> feedback) {
        callSupport.recordCompletionFeedback(projectId, completionId, feedback);
    }

    public static FreeplayConfig Config() {
        return new FreeplayConfig();
    }

    public static class FreeplayConfig {
        private String freeplayAPIKey = null;
        private String baseUrl = null;
        private ProviderConfigs providerConfigs = null;
        private Map<String, Object> llmParameters = Collections.emptyMap();
        private HttpConfig httpConfig = new HttpConfig();
        private RecordProcessor recordProcessor = null;
        private TemplateResolver templateResolver = null;

        public FreeplayConfig freeplayAPIKey(String freeplayAPIKey) {
            this.freeplayAPIKey = freeplayAPIKey;
            return this;
        }

        public FreeplayConfig customerDomain(String domain) {
            return baseUrl(String.format("https://%s.freeplay.ai/api", domain));
        }

        public FreeplayConfig baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public FreeplayConfig providerConfigs(ProviderConfigs providerConfigs) {
            this.providerConfigs = providerConfigs;
            return this;
        }

        public FreeplayConfig llmParameters(Map<String, Object> llmParameters) {
            this.llmParameters = llmParameters;
            return this;
        }

        public FreeplayConfig httpConfig(HttpConfig httpConfig) {
            this.httpConfig = httpConfig;
            return this;
        }

        public FreeplayConfig recordProcessor(RecordProcessor recordProcessor) {
            this.recordProcessor = recordProcessor;
            return this;
        }

        public FreeplayConfig templateResolver(TemplateResolver templateResolver) {
            this.templateResolver = templateResolver;
            return this;
        }

        public void validate() {
            if (templateResolver == null) {
                if (freeplayAPIKey == null || baseUrl == null) {
                    throw new FreeplayConfigurationException("Either a TemplateResolver must be configured, " +
                            "or the Freeplay API key and base URL must be configured.");
                } else {
                    templateResolver = new APITemplateResolver(baseUrl, freeplayAPIKey, httpConfig);
                }
            }
        }
    }
}
