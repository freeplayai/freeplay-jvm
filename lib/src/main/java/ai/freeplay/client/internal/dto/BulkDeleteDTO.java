package ai.freeplay.client.internal.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BulkDeleteDTO {
    private final List<String> testCaseIds;

    public BulkDeleteDTO(List<String> testCaseIds) {
        this.testCaseIds = testCaseIds;
    }

    public List<String> getTestCaseIds() {
        return testCaseIds;
    }
}
