package ai.freeplay.client.thin.resources.testruns;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TestRunResults {
    private final String id;
    private final String name;
    private final String description;
    private final Map<String, Object> summaryStatistics;

    public TestRunResults(String id, String name, String description, Map<String, Object> summaryStatistics) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.summaryStatistics = summaryStatistics;
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

    @SuppressWarnings("unused")
    public Map<String, Object> getSummaryStatistics() {
        return summaryStatistics;
    }

    @Override
    public String toString() {
        return "TestRunResults{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", summaryStatistics=" + summaryStatistics +
                '}';
    }
}
