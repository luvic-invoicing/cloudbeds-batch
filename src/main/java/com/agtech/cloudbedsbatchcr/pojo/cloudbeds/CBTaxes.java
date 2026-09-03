package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CBTaxes {
    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("data")
    private List<CBTaxData> data;

    @JsonProperty("total")
    private Integer total;
}