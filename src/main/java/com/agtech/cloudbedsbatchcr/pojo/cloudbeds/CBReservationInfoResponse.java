package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import lombok.Data;

@Data
public class CBReservationInfoResponse {
    private boolean success;
    private CBReservationInfo data;
}
