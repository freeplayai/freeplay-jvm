package ai.freeplay.client.internal.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TestRunResultsDTO {
    private String id;
    private String name;
    private String description;
    private Map<String, Object> summaryStatistics;
    private String status;

    @SuppressWarnings("unused")
    public TestRunResultsDTO() {
    }

    @SuppressWarnings("unused")
    public TestRunResultsDTO(
            String id,
            String name,
            String description,
            Map<String, Object> summaryStatistics,
            String status
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.summaryStatistics = summaryStatistics;
        this.status = status;
    }

    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public Map<String, Object> getSummaryStatistics() {
        return summaryStatistics;
    }
    public String getStatus() {
        return status;
    }

}
