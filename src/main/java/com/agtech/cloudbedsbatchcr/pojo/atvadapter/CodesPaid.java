package com.agtech.cloudbedsbatchcr.pojo.atvadapter;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CodesPaid {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String codePaidMethod1;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String codePaidMethod2;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String codePaidMethod3;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String codePaidMethod4;
    @JsonBackReference
    private Invoice invoice;
}