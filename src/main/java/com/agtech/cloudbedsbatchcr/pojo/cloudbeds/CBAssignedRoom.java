package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import lombok.Data;

import java.util.List;

@Data
public class CBAssignedRoom {
    private String reservationRoomID;
    private String roomTypeID;
    private String roomTypeName;
    private boolean roomTypeIsVirtual;
    private String subReservationID;
    private String roomTypeNameShort;
    private String startDate;
    private String endDate;
    private String adults;
    private String children;
    private List<CBDailyRate> dailyRates;
    private String roomTotal;
    private String marketName;
    private String marketCode;
}
