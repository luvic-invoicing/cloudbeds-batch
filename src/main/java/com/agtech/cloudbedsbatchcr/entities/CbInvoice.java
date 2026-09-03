package com.agtech.cloudbedsbatchcr.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table( name = "cb_invoice", schema = "cloudbeds" )
@DynamicInsert
@DynamicUpdate
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CbInvoice {

    @EmbeddedId
    private CbInvoiceId invoiceId;

    private Long consecutiveInvoice;

    @MapsId("propertyId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private CbProperties properties;

    private Date date;

    @Column(columnDefinition = "TEXT")
    private String jsonRequestCb;

    @Column(columnDefinition = "TEXT")
    private String jsonDataReservation;

    @Column(columnDefinition = "TEXT")
    private String jsonDataInvoice;

    @Column(columnDefinition = "TEXT")
    private String jsonDataAtvAdapter;


    @Column(unique = true)
    private String claveHacienda;

    private String consecutivoHacienda;

    private String othersMessage;

    @Enumerated(EnumType.STRING)
    private CbStateInvoice state;

    @Column(columnDefinition = "TEXT")
    private String respuestaHacienda;
}
