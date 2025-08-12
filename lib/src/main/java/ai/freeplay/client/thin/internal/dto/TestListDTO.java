package ai.freeplay.client.thin.internal.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TestListDTO {
    private String datasetName;
    private boolean includeOutputs;

    private String testRunName;

    private String testRunDescription;

    private String flavorName;

    private List<UUID> targetEvaluationIds;

    @SuppressWarnings("unused")
    public TestListDTO() {
    }

    public TestListDTO(String datasetName, boolean includeOutputs, String testRunName, String testRunDescription, String flavorName, List<UUID> targetEvaluationIds) {
        this.datasetName = datasetName;
        this.includeOutputs = includeOutputs;
        this.testRunName = testRunName;
        this.testRunDescription = testRunDescription;
        this.flavorName = flavorName;
        this.targetEvaluationIds = targetEvaluationIds;
    }

    @SuppressWarnings("unused")
    public String getDatasetName() {
        return datasetName;
    }

    @SuppressWarnings("unused")
    public boolean getIncludeOutputs() {
        return includeOutputs;
    }

    public String getTestRunName() {
        return testRunName;
    }

    @SuppressWarnings("unused")
    public String getTestRunDescription() {
        return testRunDescription;
    }

    @SuppressWarnings("unused")
    public String getFlavorName() {
        return flavorName;
    }

    @SuppressWarnings("unused")
    public List<UUID> targetEvaluationIds() { return targetEvaluationIds; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestListDTO that = (TestListDTO) o;
        return Objects.equals(datasetName, that.datasetName) && Objects.equals(includeOutputs, that.includeOutputs) && Objects.equals(testRunName, that.getTestRunName()) && Objects.equals(testRunDescription, that.testRunDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(datasetName, includeOutputs, testRunName, testRunDescription);
    }

    @Override
    public String toString() {
        return "TestListDTO{" +
                "datasetName='" + datasetName + '\'' +
                ", includeOutputs=" + includeOutputs +
                ", testRunName='" + testRunName + '\'' +
                ", testRunDescription='" + testRunDescription + '\'' +
                '}';
    }
}
