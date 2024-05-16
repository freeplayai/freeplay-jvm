package ai.freeplay.client.thin.internal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestListDTO {
    private final String testListName;
    private final boolean includeOutputs;

    private final String name;

    private final String description;

    public TestListDTO(String testListName, boolean includeOutputs, String name, String description) {
        this.testListName = testListName;
        this.includeOutputs = includeOutputs;
        this.name = name;
        this.description = description;
    }

    @SuppressWarnings("unused")
    @JsonProperty("testlist_name")
    public String getTestListName() {
        return testListName;
    }

    @SuppressWarnings("unused")
    @JsonProperty("include_test_case_outputs")
    public boolean getIncludeOutputs() {
        return includeOutputs;
    }

    @SuppressWarnings("unused")
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }
}
