package com.agtech.cloudbedsbatchcr.pojo.webhook;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoiceRequest {
    // Fiscal Documents webhook payload fields
    @JsonAlias("propertyId")
    private String propertyIdText;
    private String documentKind;
    private String id;
    private String status;
    private Double timestamp;
}
