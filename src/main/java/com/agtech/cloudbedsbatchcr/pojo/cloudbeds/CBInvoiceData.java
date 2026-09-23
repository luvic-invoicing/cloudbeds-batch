package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import lombok.Data;

import java.util.List;

@Data
public class CBInvoiceData {
    private String reservationIdentifier;
    private Long number;
    private List<CBInvoiceItem> items;
    private List<CBInvoiceTax> taxes;
    private List<CBInvoiceFee> fees;
    private String status;
    private CBInvoiceBilledTo billedTo;
}
