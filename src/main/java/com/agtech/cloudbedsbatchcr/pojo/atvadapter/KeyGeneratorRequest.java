package com.agtech.cloudbedsbatchcr.pojo.atvadapter;

import lombok.Data;

@Data
public class KeyGeneratorRequest
{
    private int matriz;

    private int puntoVenta;

    private String tipoDoc;

    private String numIdentification;

    private Boolean prodSequence;
}