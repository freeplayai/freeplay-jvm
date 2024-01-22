package ai.freeplay.client.thin.internal.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Collection;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Templates {
    private Collection<Template> templates;

    public Templates() {
    }

    public Templates(Collection<Template> templates) {
        this.templates = templates;
    }

    public Collection<Template> getTemplates() {
        return templates;
    }
}
