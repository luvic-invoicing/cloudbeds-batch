package com.agtech.cloudbedsbatchcr.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table( name = "cb_reservation", schema = "cloudbeds" )
@DynamicInsert
@DynamicUpdate
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CbReservation {

    @EmbeddedId
    private CbReservationId cbReservationId;

    @MapsId("propertyId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private CbProperties properties;

    private Date date;

    private String fiscalKey;

    @Column(columnDefinition = "TEXT")
    private String jsonData;

    @Column(columnDefinition = "TEXT")
    private String jsonDataInvoice;
}
