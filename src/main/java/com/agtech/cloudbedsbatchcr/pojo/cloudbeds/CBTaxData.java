package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CBTaxData {
    @JsonProperty("name")
    private String name;

    @JsonProperty("code")
    private String code;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("amountAdult")
    private String amountAdult;

    @JsonProperty("amountChild")
    private String amountChild;

    @JsonProperty("amountType")
    private String amountType;

    @JsonProperty("inclusiveOrExclusive")
    private String inclusiveOrExclusive;

    @JsonProperty("isDeleted")
    private Boolean isDeleted;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("expiredAt")
    private String expiredAt;

    @JsonProperty("childId")
    private String childId;

    @JsonProperty("type")
    private String type;

    @JsonProperty("taxID")
    private String taxID;

    @JsonProperty("kind")
    private String kind;
}
