package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CBReservation {
    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("data")
    private CBReservationData data;
}
