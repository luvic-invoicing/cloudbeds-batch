package com.agtech.cloudbedsbatchcr.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table( name = "cb_account", schema = "cloudbeds" )
@DynamicInsert
@DynamicUpdate
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CbAccount {

    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )
    private Long accountId;

    private String apiUrl;

    private String apiAccountingUrl;

    private Boolean enabled;

    private String apiKey;

    private String documentUrl;

    private Date lastExchangeDate;

    //C:Compra, V: Venta
    private String tcIndicator;

    private Double exchange;
}
