package com.agtech.cloudbedsbatchcr.pojo.webhook;

import lombok.Data;

@Data
public class InvoiceRequest {
    private Long propertyID;
    private String propertyID_str;
    private String reservationID;
    private String invoiceID;
    private String version;
    private String event;
    private Double timestamp;
}
