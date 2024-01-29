package ai.freeplay.client.thin.internal.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestListPayload {
    private final String testListName;

    public TestListPayload(String testListName) {
        this.testListName = testListName;
    }

    @SuppressWarnings("unused")
    @JsonProperty("testlist_name")
    public String getTestListName() {
        return testListName;
    }
}
