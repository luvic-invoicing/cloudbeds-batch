package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import lombok.Data;

@Data
public class CBGuestInfo {
    private String guestID;
    private String guestFirstName;
    private String guestLastName;
    private String guestGender;
    private String guestEmail;
    private String guestPhone;
    private String guestCellPhone;
    private String guestCountry;
    private String guestAddress;
    private String guestAddress2;
    private String guestCity;
    private String guestZip;
    private String guestState;
    private String guestStatus;
    private String guestBirthdate;
    private String guestDocumentType;
    private String guestDocumentNumber;
    private String guestDocumentIssueDate;
    private String guestDocumentIssuingCountry;
    private String guestDocumentExpirationDate;
    private boolean assignedRoom;
    private String roomTypeName;
    private boolean isMainGuest;
    private boolean isAnonymized;
    private String taxID;
    private String companyTaxID;
    private String companyName;
}
