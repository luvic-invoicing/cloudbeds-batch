package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CBBalanceDetailed {

    @JsonProperty("suggestedDeposit")
    private Double suggestedDeposit;

    @JsonProperty("subTotal")
    private Double subTotal;

    @JsonProperty("additionalItems")
    private Double additionalItems;

    @JsonProperty("taxesFees")
    private Double taxesFees;

    @JsonProperty("grandTotal")
    private Double grandTotal;

    @JsonProperty("paid")
    private Double paid;
}
