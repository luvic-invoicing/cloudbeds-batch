package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import lombok.Data;

import java.util.ArrayList;

@Data
public class CBPropertyData {

    private String propertyID;
    private String propertyName;
    private ArrayList<CBPropertyImage> propertyImage;
    private String propertyDescription;
    private CBPropertyCurrency propertyCurrency;
    private String propertyPrimaryLanguage;
    private ArrayList<CBPropertyAdditionalPhoto> propertyAdditionalPhotos;
    private String propertyPhone;
    private String propertyEmail;
    private CBPropertyAddress propertyAddress;
    private CBPropertyPolicy propertyPolicy;
    private ArrayList<String> propertyAmenities;
    private String taxID;
    private String taxID2;
    private String companyLegalName;
}
