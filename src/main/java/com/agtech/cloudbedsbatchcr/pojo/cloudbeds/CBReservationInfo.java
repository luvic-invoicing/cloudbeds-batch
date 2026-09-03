package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import lombok.Data;

import java.util.Map;

@Data
public class CBReservationInfo {
    private String propertyID;
    private String guestName;
    private String guestEmail;
    private boolean isAnonymized;
    private Map<String, CBGuestInfo> guestList;
    private String reservationID;
    private String dateCreated;
    private String dateModified;
    private String source;
    private String sourceID;
    private String thirdPartyIdentifier;
    private String status;
    private double total;
    private double balance;
    private CBBalanceDetailed balanceDetailed;
    private java.util.List<CBAssignedRoom> assigned;
    private java.util.List<CBAssignedRoom> unassigned;
    private String startDate;
    private String endDate;
    private String allotmentBlockCode;
    private String orderId;
    private String origin;
}
