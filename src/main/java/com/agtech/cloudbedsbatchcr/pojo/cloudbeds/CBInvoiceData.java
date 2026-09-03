package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import lombok.Data;

import java.util.List;

@Data
public class CBInvoiceData {
    private String reservationIdentifier;
    private String reservationID;
    private String invoiceID;
    private String userID;
    private String prefix;
    private Long number;
    private String suffix;
    private String documentIssueDate;
    private List<CBInvoiceItem> items;
    private List<CBInvoiceTax> taxes;
    private List<CBInvoiceFee> fees;
    private String status;
    private CBInvoiceBilledTo billedTo;
}
