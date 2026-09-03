package com.agtech.cloudbedsbatchcr.pojo.webhook;

import lombok.Data;

@Data
public class StatusChanged {
    private String version;
    private Double timestamp;
    private String event;
    private Long propertyID;
    private String reservationID;

    //Allowed values: confirmed, not_confirmed, canceled, checked_in, checked_out, no_show
    private String status;
}
