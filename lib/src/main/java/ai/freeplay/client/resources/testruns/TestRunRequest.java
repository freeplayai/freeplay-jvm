package ai.freeplay.client.resources.testruns;

import java.util.List;
import java.util.UUID;

public class TestRunRequest {
    private final String projectId;
    private final String datasetName;
    private final boolean includeOutputs;
    private final String name;
    private final String description;
    private final String flavorName;
    private final List<UUID> targetEvaluationIds;

    private TestRunRequest(Builder builder) {
        this.projectId = builder.projectId;
        this.datasetName = builder.datasetName;
        this.includeOutputs = builder.includeOutputs;
        this.name = builder.name;
        this.description = builder.description;
        this.flavorName = builder.flavorName;
        this.targetEvaluationIds = builder.targetEvaluationIds;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public boolean includeOutputs() {
        return includeOutputs;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getFlavorName() {
        return flavorName;
    }

    public List<UUID> getTargetEvaluationIds() { return targetEvaluationIds; }

    public static class Builder {
        private final String projectId;
        private final String datasetName;
        private boolean includeOutputs = false;
        private String name = null;
        private String description = null;
        private String flavorName = null;
        private List<UUID> targetEvaluationIds = null;

        public Builder(String projectId, String datasetName) {
            this.projectId = projectId;
            this.datasetName = datasetName;
        }

        public Builder includeOutputs(boolean includeOutputs) {
            this.includeOutputs = includeOutputs;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder flavorName(String flavorName) {
            this.flavorName = flavorName;
            return this;
        }

        public Builder targetEvaluationIds(List<UUID> targetEvaluationIds) {
            this.targetEvaluationIds = targetEvaluationIds;
            return this;
        }

        public TestRunRequest build() {
            return new TestRunRequest(this);
        }
    }
}
