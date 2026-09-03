package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.models.auth.In;
import lombok.Data;

@Data
public class CBReservationRoom {
    @JsonProperty("roomTypeName")
    private String roomTypeName;

    @JsonProperty("guestName")
    private String guestName;

    @JsonProperty("startDate")
    private String startDate;

    @JsonProperty("endDate")
    private String endDate;

    @JsonProperty("adults")
    private Integer adults;

    @JsonProperty("children")
    private Integer children;

    @JsonProperty("nights")
    private Integer nights;

    @JsonProperty("roomTotal")
    private Double roomTotal;

    @JsonProperty("roomID")
    private String roomID;

    @JsonProperty("roomName")
    private String roomName;

    @JsonProperty("roomTypeID")
    private String roomTypeID;

    @JsonProperty("isAnonymized")
    private Boolean isAnonymized;
}
