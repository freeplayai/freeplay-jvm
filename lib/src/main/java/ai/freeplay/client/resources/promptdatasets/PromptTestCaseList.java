package ai.freeplay.client.resources.promptdatasets;

import ai.freeplay.client.resources.datasets.Pagination;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PromptTestCaseList {
    private List<PromptTestCase> data;
    private Pagination pagination;

    public PromptTestCaseList() {
    }

    public void setData(List<PromptTestCase> data) { this.data = data; }
    public void setPagination(Pagination pagination) { this.pagination = pagination; }

    public List<PromptTestCase> getData() { return data; }
    public Pagination getPagination() { return pagination; }
}
