package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import lombok.Data;

@Data
public class CBInvoiceResponse {
    private boolean success;
    private CBInvoiceData data;
    private String statusMessage;
    private Integer statusCode;
    private String errorCode;
}
