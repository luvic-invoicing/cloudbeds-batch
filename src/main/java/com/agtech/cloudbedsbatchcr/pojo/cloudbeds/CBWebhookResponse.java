package com.agtech.cloudbedsbatchcr.pojo.cloudbeds;

import lombok.Data;

@Data
public class CBWebhookResponse {

    private Boolean success;
    private CBWebhookData data;
}
