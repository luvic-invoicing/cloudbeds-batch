package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CBMainGuestDetails {

    @JsonProperty("guestFirstName")
    private String guestFirstName;

    @JsonProperty("guestLastName")
    private String guestLastName;

    @JsonProperty("guestGender")
    private String guestGender;

    @JsonProperty("guestEmail")
    private String guestEmail;

    @JsonProperty("guestPhone")
    private String guestPhone;

    @JsonProperty("guestCellPhone")
    private String guestCellPhone;

    @JsonProperty("guestAddress")
    private String guestAddress;

    @JsonProperty("guestAddress2")
    private String guestAddress2;

    @JsonProperty("guestCity")
    private String guestCity;

    @JsonProperty("guestState")
    private String guestState;

    @JsonProperty("guestZip")
    private String guestZip;

    @JsonProperty("guestCountry")
    private String guestCountry;

    @JsonProperty("taxID")
    private String taxID;

    @JsonProperty("companyTaxID")
    private String companyTaxID;

    @JsonProperty("companyName")
    private String companyName;

    @JsonProperty("isAnonymized")
    private Boolean isAnonymized;

}
