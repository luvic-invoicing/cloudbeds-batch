package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import lombok.Data;

@Data
public class CBReservationAdjustment {
    private String adjustmentDescription;
    private String adjustmentRoomName;
    private String adjustmentDateTime;
    private String adjustmentDateTimeUTC;
    private Double adjustmentAmount;
}
