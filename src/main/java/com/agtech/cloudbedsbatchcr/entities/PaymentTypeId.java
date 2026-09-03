package com.agtech.cloudbedsbatchcr.entities;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Data
public class PaymentTypeId implements Serializable {
    @Column(name = "payment_type", nullable = false)
    private String paymentType;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;
}
