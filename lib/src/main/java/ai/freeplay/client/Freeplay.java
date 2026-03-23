package ai.freeplay.client;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.internal.CallSupport;
import ai.freeplay.client.resources.feedback.CustomerFeedback;
import ai.freeplay.client.resources.metadata.Metadata;
import ai.freeplay.client.resources.prompts.Prompts;
import ai.freeplay.client.resources.recordings.Recordings;
import ai.freeplay.client.resources.sessions.Sessions;
import ai.freeplay.client.resources.agentdatasets.AgentDatasets;
import ai.freeplay.client.resources.promptdatasets.PromptDatasets;
import ai.freeplay.client.resources.testruns.TestRuns;
import ai.freeplay.client.resources.traces.Traces;

public class Freeplay {

    private final Sessions sessions;
    private final Prompts prompts;
    private final Recordings recordings;
    private final TestRuns testRuns;
    private final CustomerFeedback customerFeedback;
    private final Metadata metadata;
    private final PromptDatasets promptDatasets;
    private final AgentDatasets agentDatasets;
    private final Traces traces;

    public Freeplay(FreeplayConfig config) {
        config.validate();
        CallSupport callSupport = new CallSupport(
                config.httpConfig,
                config.templateResolver,
                config.baseUrl,
                config.freeplayAPIKey
        );
        sessions = new Sessions(callSupport);
        prompts = new Prompts(callSupport);
        recordings = new Recordings(callSupport);
        testRuns = new TestRuns(callSupport);
        customerFeedback = new CustomerFeedback(callSupport);
        metadata = new Metadata(callSupport);
        promptDatasets = new PromptDatasets(callSupport);
        agentDatasets = new AgentDatasets(callSupport);
        traces = new Traces(callSupport);
    }

    public Sessions sessions() {
        return sessions;
    }

    public Prompts prompts() {
        return prompts;
    }

    public Recordings recordings() {
        return recordings;
    }

    public TestRuns testRuns() {
        return testRuns;
    }

    public CustomerFeedback customerFeedback() {
        return customerFeedback;
    }

    public Metadata metadata() {
        return metadata;
    }

    public PromptDatasets promptDatasets() {
        return promptDatasets;
    }

    public AgentDatasets agentDatasets() {
        return agentDatasets;
    }

    public Traces traces() {
        return traces;
    }

    public static FreeplayConfig Config() {
        return new FreeplayConfig();
    }

    public static class FreeplayConfig {
        private String freeplayAPIKey = null;
        private String baseUrl = null;
        private HttpConfig httpConfig = new HttpConfig();
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

        public FreeplayConfig httpConfig(HttpConfig httpConfig) {
            this.httpConfig = httpConfig;
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
