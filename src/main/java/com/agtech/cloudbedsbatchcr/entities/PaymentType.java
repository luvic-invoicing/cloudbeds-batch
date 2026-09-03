package com.agtech.cloudbedsbatchcr.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;

@Entity
@Table( name = "cb_payment_type", schema = "cloudbeds" )
@DynamicInsert
@DynamicUpdate
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentType {

    @EmbeddedId
    private PaymentTypeId paymentTypeId;

    @MapsId("propertyId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private CbProperties properties;

    private String fiscalCode;
}
