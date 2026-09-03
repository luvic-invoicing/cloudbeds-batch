package com.agtech.cloudbedsbatchcr.entities;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;

@Embeddable
@Data
public class CbInvoiceId implements Serializable {
    @Column(name = "invoice_id", nullable = false)
    private String invoiceId;

    @Column(name = "reservation_id", nullable = false)
    private String reservationId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Enumerated(EnumType.STRING)
    private CbTypeInvoice type;
}
