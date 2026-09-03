package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import lombok.Data;

import java.util.List;

@Data
public class CBGuestData {
    private String guestID;
    private String firstName;
    private String lastName;
    private String gender;
    private String email;
    private String phone;
    private String cellPhone;
    private String country;
    private String address;
    private String address2;
    private String city;
    private String zip;
    private String state;
    private String birthDate;
    private String documentType;
    private String documentNumber;
    private String documentIssueDate;
    private String documentIssuingCountry;
    private String documentExpirationDate;
    private List<Object> customFields; // Cambia el tipo según la estructura real
    private String specialRequests;
    private Boolean isAnonymized;
    private String taxID;
    private String companyTaxID;
    private String companyName;
    private Object guestOptIn; // Cambia el tipo según la estructura real
    private Boolean isMerged;
    private String newGuestID;
}
