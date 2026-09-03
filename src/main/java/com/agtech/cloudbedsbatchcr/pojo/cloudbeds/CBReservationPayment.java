package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class CBReservationPayment {

    @JsonProperty("paymentType")
    private String paymentType;

    @JsonProperty("paymentDescription")
    private String paymentDescription;

    @JsonProperty("paymentDateTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date paymentDateTime;

    @JsonProperty("paymentDateTimeUTC")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date paymentDateTimeUTC;

    @JsonProperty("paymentAmount")
    private Double paymentAmount;
}
