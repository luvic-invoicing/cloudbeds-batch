package com.agtech.cloudbedsbatchcr.pojo.atvadapter;

import lombok.Data;

@Data
public class KeyGeneratorResponse
{
    private String consecutiveNumber;

    private Long currentConsecutiveNumber;

    private String securityCode;

    private String voucherKey;
}