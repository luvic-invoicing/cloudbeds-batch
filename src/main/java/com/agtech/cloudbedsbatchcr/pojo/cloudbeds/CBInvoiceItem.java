package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import lombok.Data;

import java.util.List;

@Data
public class CBInvoiceItem {
    private String description;
    private String type;
    private Double quantity;
    private String totalAmount;
    private String netAmount;
    private String currency;
    private List<CBInvoiceTax> taxes;
    private List<CBInvoiceFee> fees;
}
