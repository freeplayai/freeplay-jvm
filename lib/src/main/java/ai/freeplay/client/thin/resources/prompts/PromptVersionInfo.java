package ai.freeplay.client.thin.resources.prompts;

import java.util.Objects;

public class PromptVersionInfo {
    private final String promptTemplateVersionId;
    private final String environment;

    public PromptVersionInfo(String promptTemplateVersionId, String environment) {
        this.promptTemplateVersionId = promptTemplateVersionId;
        this.environment = environment;
    }

    public String getPromptTemplateVersionId() {
        return promptTemplateVersionId;
    }

    public String getEnvironment() {
        return environment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PromptVersionInfo that = (PromptVersionInfo) o;
        return Objects.equals(promptTemplateVersionId, that.promptTemplateVersionId) && Objects.equals(environment, that.environment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(promptTemplateVersionId, environment);
    }

    @Override
    public String toString() {
        return "PromptVersionInfo{" +
                "promptTemplateVersionId='" + promptTemplateVersionId + '\'' +
                ", environment='" + environment + '\'' +
                '}';
    }
}
