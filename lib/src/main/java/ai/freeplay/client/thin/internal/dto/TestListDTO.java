package ai.freeplay.client.thin.internal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestListDTO {
    private final String testListName;
    private final boolean includeOutputs;

    public TestListDTO(String testListName, boolean includeOutputs) {
        this.testListName = testListName;
        this.includeOutputs = includeOutputs;
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
}
