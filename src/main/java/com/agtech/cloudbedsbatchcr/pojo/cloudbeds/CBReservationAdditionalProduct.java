package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties
public class CBReservationAdditionalProduct {

    @JsonProperty("productID")
    private Integer productID;

    @JsonProperty("soldProductID")
    private Integer soldProductID;

    @JsonProperty("sold_product_id")
    private String soldProductId;

    @JsonProperty("itemCode")
    private String itemCode;

    @JsonProperty("productName")
    private String productName;

    @JsonProperty("productPrice")
    private Double productPrice;

    @JsonProperty("productQuantity")
    private Integer productQuantity;

    @JsonProperty("productSubTotal")
    private Double productSubTotal;

    private Double discounts;

    @JsonProperty("productFees")
    private Double productFees;

    @JsonProperty("productTaxes")
    private Double productTaxes;

    @JsonProperty("productTotal")
    private Double productTotal;

    @JsonProperty("productNote")
    private String productNote;
}
