package com.agtech.cloudbedsbatchcr.entities;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Data
public class CbReservationId implements Serializable {
    @Column(name = "reservation_id", nullable = false)
    private String reservationId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;
}
