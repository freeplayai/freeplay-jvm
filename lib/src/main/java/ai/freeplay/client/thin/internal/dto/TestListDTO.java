package ai.freeplay.client.thin.internal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestListDTO {
    private final String testListName;

    public TestListDTO(String testListName) {
        this.testListName = testListName;
    }

    @SuppressWarnings("unused")
    @JsonProperty("testlist_name")
    public String getTestListName() {
        return testListName;
    }
}
