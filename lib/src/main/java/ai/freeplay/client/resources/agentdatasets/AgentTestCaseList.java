package ai.freeplay.client.resources.agentdatasets;

import ai.freeplay.client.resources.datasets.Pagination;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@SuppressWarnings("unused")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AgentTestCaseList {
    private List<AgentTestCase> data;
    private Pagination pagination;

    public AgentTestCaseList() {
    }

    public void setData(List<AgentTestCase> data) { this.data = data; }
    public void setPagination(Pagination pagination) { this.pagination = pagination; }

    public List<AgentTestCase> getData() { return data; }
    public Pagination getPagination() { return pagination; }
}
