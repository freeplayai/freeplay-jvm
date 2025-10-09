package ai.freeplay.client.thin.internal.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@SuppressWarnings("unused")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdateVersionEnvironmentsDTO {
    private List<String> environments;

    public UpdateVersionEnvironmentsDTO() {
    }

    public UpdateVersionEnvironmentsDTO(List<String> environments) {
        this.environments = environments;
    }

    public List<String> getEnvironments() {
        return environments;
    }
}
