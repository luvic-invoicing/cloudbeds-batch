package com.agtech.cloudbedsbatchcr.pojo.atvadapter;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class InvoiceOtherCharges {
    String documentType;
    String detail;
    double percentaje;
    double chargeAmount;
}
