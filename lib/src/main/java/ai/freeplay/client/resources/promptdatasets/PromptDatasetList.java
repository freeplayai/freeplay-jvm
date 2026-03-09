package ai.freeplay.client.resources.promptdatasets;

import ai.freeplay.client.resources.datasets.Pagination;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PromptDatasetList {
    private List<PromptDataset> data;
    private Pagination pagination;

    public PromptDatasetList() {
    }

    public void setData(List<PromptDataset> data) { this.data = data; }
    public void setPagination(Pagination pagination) { this.pagination = pagination; }

    public List<PromptDataset> getData() { return data; }
    public Pagination getPagination() { return pagination; }
}
