package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CBReservationTax {

    @JsonProperty("taxName")
    private String taxName;

    @JsonProperty("taxAmount")
    private Double taxAmount;
}
