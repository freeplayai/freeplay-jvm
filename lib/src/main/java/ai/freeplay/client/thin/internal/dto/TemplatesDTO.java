package ai.freeplay.client.thin.internal.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Collection;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TemplatesDTO {
    private Collection<TemplateDTO> templates;

    @SuppressWarnings("unused")
    public TemplatesDTO() {
    }

    public TemplatesDTO(Collection<TemplateDTO> templates) {
        this.templates = templates;
    }

    public Collection<TemplateDTO> getTemplates() {
        return templates;
    }
}
