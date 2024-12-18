package ai.freeplay.client.thin.resources.testruns;

public class TestRunRequest {
    private final String projectId;
    private final String testList;
    private final boolean includeOutputs;
    private final String name;
    private final String description;
    private final String flavorName;

    private TestRunRequest(Builder builder) {
        this.projectId = builder.projectId;
        this.testList = builder.testList;
        this.includeOutputs = builder.includeOutputs;
        this.name = builder.name;
        this.description = builder.description;
        this.flavorName = builder.flavorName;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getTestList() {
        return testList;
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

    public static class Builder {
        private final String projectId;
        private final String testList;
        private boolean includeOutputs = false;
        private String name = null;
        private String description = null;
        private String flavorName = null;

        public Builder(String projectId, String testList) {
            this.projectId = projectId;
            this.testList = testList;
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

        public TestRunRequest build() {
            return new TestRunRequest(this);
        }
    }
}
