package com.agtech.cloudbedsbatchcr.entities;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "cb_log", schema = "cloudbeds" )
@DynamicInsert
@DynamicUpdate
@Data
public class CbLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private CbAccount cbAccount;

    private Date logDate;

    private String description;

    private String error;
}
