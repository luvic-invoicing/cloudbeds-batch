package com.agtech.cloudbedsbatchcr.pojo.webhook;

import lombok.Data;

@Data
public class AppState {
    private String version;
    private float timestamp;
    private String event;
    private Long propertyID;
    private float clientID;
    private String oldState;
    private String newState;
}
