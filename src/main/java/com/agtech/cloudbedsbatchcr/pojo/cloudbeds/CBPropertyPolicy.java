package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import lombok.Data;

@Data
public class CBPropertyPolicy {
    private String propertyCheckInTime;
    private String propertyCheckOutTime;
    private Boolean propertyLateCheckOutAllowed;
    private String propertyLateCheckOutType;
    private String propertyLateCheckOutValue;
    private String propertyTermsAndConditions;
}
