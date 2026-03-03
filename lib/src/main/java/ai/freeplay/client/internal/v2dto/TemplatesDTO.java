package ai.freeplay.client.internal.v2dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Collection;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TemplatesDTO {
    private Collection<TemplateDTO> promptTemplates;

    @SuppressWarnings("unused")
    public TemplatesDTO() {
    }

    public TemplatesDTO(Collection<TemplateDTO> templates) {
        this.promptTemplates = templates;
    }

    public Collection<TemplateDTO> getPromptTemplates() {
        return promptTemplates;
    }
}
