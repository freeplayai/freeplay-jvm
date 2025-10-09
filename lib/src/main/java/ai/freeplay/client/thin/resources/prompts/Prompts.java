package ai.freeplay.client.thin.resources.prompts;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.media.MediaInputCollection;
import ai.freeplay.client.thin.LLMAdapters;
import ai.freeplay.client.thin.LLMAdapters.LLMAdapter;
import ai.freeplay.client.thin.internal.ThinCallSupport;
import ai.freeplay.client.thin.internal.v2dto.TemplateDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;

@SuppressWarnings("unused")
public class Prompts {

    private final ThinCallSupport callSupport;

    public Prompts(ThinCallSupport callSupport) {
        this.callSupport = callSupport;
    }

    public CompletableFuture<TemplatePrompt> get(
            String projectId,
            String templateName,
            String environment
    ) {
        return callSupport
                .getPrompt(projectId, templateName, environment)
                .thenApply(template -> getTemplateFromDTO(projectId, environment, template));
    }

    public CompletableFuture<TemplatePrompt> getByVersionId(
            String projectId,
            String templateId,
            String templateVersionId
    ) {
        return callSupport
                .getPromptByVersionId(projectId, templateId, templateVersionId)
                .thenApply(template -> getTemplateFromDTO(projectId, null, template));
    }

    /**
     * @deprecated use {@link #getFormatted(GetFormattedRequest)} instead.
     */
    @Deprecated
    public <LLMFormat> CompletableFuture<FormattedPrompt<LLMFormat>> getFormatted(
            String projectId,
            String templateName,
            String environment,
            Map<String, Object> variables,
            String flavorName
    ) {
        return getBound(projectId, templateName, environment, variables, null, null)
                .thenApply(boundPrompt -> boundPrompt.format(flavorName));
    }

    /**
     * @deprecated use {@link #getFormatted(GetFormattedRequest)} instead.
     */
    @Deprecated
    public <LLMFormat> CompletableFuture<FormattedPrompt<LLMFormat>> getFormatted(
            String projectId,
            String templateName,
            String environment,
            Map<String, Object> variables
    ) {
        return getBound(projectId, templateName, environment, variables, null, null)
                .thenApply(BoundPrompt::format);
    }

    public <LLMFormat> CompletableFuture<FormattedPrompt<LLMFormat>> getFormatted(GetFormattedRequest request) {
        return getBound(request.getProjectId(), request.getTemplateName(), request.getEnvironment(),
                request.getVariables(), request.getHistory(), request.getMediaInputs())
                .thenApply(boundPrompt -> boundPrompt.format(request.getFlavorName()));
    }

    public <LLMFormat> CompletableFuture<FormattedPrompt<LLMFormat>> getFormattedByVersionId(
            String projectId,
            String templateId,
            String templateVersionId,
            Map<String, Object> variables,
            String flavorName
    ) {
        return this.getByVersionId(projectId, templateId, templateVersionId)
                .thenApply(templatePrompt -> templatePrompt.bind(new TemplatePrompt.BindRequest(variables)))
                .thenApply(boundPrompt -> boundPrompt.format(flavorName));
    }

    @SuppressWarnings("SameParameterValue")
    private CompletableFuture<BoundPrompt> getBound(
            String projectId,
            String templateName,
            String environment,
            Map<String, Object> variables,
            List<ChatMessage> history,
            MediaInputCollection mediaInputs
    ) {
        return get(projectId, templateName, environment)
                .thenApply(templatePrompt -> templatePrompt.bind(new TemplatePrompt.BindRequest(variables).history(history).mediaInputs(mediaInputs)));
    }

    private void validateReturnedTemplate(TemplateDTO template) {
        if (template.getMetadata().getFlavor() == null) {
            throw new FreeplayConfigurationException(
                    "Flavor must be configured in the Freeplay UI. Unable to fulfill request.");
        }
        if (template.getMetadata().getModel() == null) {
            throw new FreeplayConfigurationException(
                    "Model must be configured in the Freeplay UI. Unable to fulfill request.");
        }
    }

    public static class GetFormattedRequest {
        private final String projectId;
        private final String templateName;
        private final String environment;
        private final Map<String, Object> variables;
        private List<ChatMessage> history;
        private String flavorName;

        private MediaInputCollection mediaInputs;

        public GetFormattedRequest(String projectId, String templateName, String environment, Map<String, Object> variables) {
            this.projectId = projectId;
            this.templateName = templateName;
            this.environment = environment;
            this.variables = variables;
        }

        public GetFormattedRequest flavorName(String flavorName) {
            this.flavorName = flavorName;
            return this;
        }

        public GetFormattedRequest history(List<ChatMessage> history) {
            this.history = history;
            return this;
        }

        public GetFormattedRequest mediaInputs(MediaInputCollection mediaInputs) {
            this.mediaInputs = mediaInputs;
            return this;
        }

        public String getProjectId() {
            return projectId;
        }

        public String getTemplateName() {
            return templateName;
        }

        public String getEnvironment() {
            return environment;
        }

        public Map<String, Object> getVariables() {
            return variables;
        }

        public String getFlavorName() {
            return flavorName;
        }

        public List<ChatMessage> getHistory() {
            return history;
        }

        public MediaInputCollection getMediaInputs() {
            return mediaInputs;
        }

        @Override
        public String toString() {
            return "GetFormattedRequest{" +
                    "projectId='" + projectId + '\'' +
                    ", templateName='" + templateName + '\'' +
                    ", environment='" + environment + '\'' +
                    ", variables=" + variables +
                    ", history=" + history +
                    ", flavorName='" + flavorName + '\'' +
                    ", mediaInputs=" + mediaInputs +
                    '}';
        }
    }

    public CompletableFuture<TemplateVersionResponse> createVersion(CreateVersionRequest request) {
        return callSupport.createPromptVersion(
                request.getProjectId(),
                request.getPromptTemplateName(),
                request.getTemplateMessages(),
                request.getModel(),
                request.getProvider().getName(),
                request.getVersionName(),
                request.getVersionDescription(),
                request.getLlmParameters(),
                request.getToolSchema(),
                request.getEnvironments()
        );
    }

    public CompletableFuture<Void> updateVersionEnvironments(
            String projectId,
            String promptTemplateId,
            String prompteTemplateVersionId,
            List<String> environments
    ) {
        return callSupport.updateTemplateVersionEnvironments(
                projectId, promptTemplateId, prompteTemplateVersionId, environments
        );
    }

    private TemplatePrompt getTemplateFromDTO(String projectId, String environment, TemplateDTO template) {
        validateReturnedTemplate(template);

        LLMAdapter<?> llmAdapter = LLMAdapters.adapterForFlavor(template.getMetadata().getFlavor());
        String model = template.getMetadata().getModel();
        HashMap<String, Object> params = new HashMap<>(template.getMetadata().getParams());
        params.remove("model");

        List<ChatMessage> messages = template.getContent().stream().map(message -> {
            if (message.isKind()) {
                return new KindMessage(message.getKind());
            } else {
                List<TemplateDTO.MediaSlot> parsedMediaSlots = message.getMediaSlots();
                List<MediaSlot> mediaSlots = List.of();
                if (parsedMediaSlots != null) {
                    mediaSlots = parsedMediaSlots.stream()
                            .map((slot ->
                                    new MediaSlot(MediaType.valueOf(slot.getType().toUpperCase()), slot.getPlaceholderName())))
                            .collect(toList());
                }
                return new ChatMessage(message.getRole(), message.getContent(), mediaSlots);
            }
        }).collect(toList());

        PromptInfo promptInfo = new PromptInfo(
                template.getPromptTemplateId(),
                template.getPromptTemplateVersionId(),
                template.getPromptTemplateName(),
                environment,
                params,
                llmAdapter.getProvider(),
                model,
                template.getMetadata().getFlavor()
        ).providerInfo(template.getMetadata().getProviderInfo());

        TemplatePrompt templatePrompt = new TemplatePrompt(promptInfo, messages, template.getToolSchema());

        if (template.getOutputSchema() != null) {
            templatePrompt.outputSchema(template.getOutputSchema());
        }

        return templatePrompt;
    }
}
