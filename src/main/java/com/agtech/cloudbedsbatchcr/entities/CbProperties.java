package com.agtech.cloudbedsbatchcr.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;

@Entity
@Table( name = "cb_properties", schema = "cloudbeds" )
@DynamicInsert
@DynamicUpdate
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CbProperties {

    @Id
    private Long propertyId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", nullable = false)
    private CbAccount cbAccount;

    private String taxIdentificacion;

    private String companyName;

    private String currency;

    private Boolean enable;

    //Punta de venta
    private String posNumber;

    // Numero de sucusal
    private String headOfficeNumber;

    private String sequenceNameBill;

    private String productCodeDefault;
}
