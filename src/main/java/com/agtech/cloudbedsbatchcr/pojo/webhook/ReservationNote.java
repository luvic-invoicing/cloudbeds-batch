package com.agtech.cloudbedsbatchcr.pojo.webhook;

import lombok.Data;

@Data
public class ReservationNote {

    private String clave;

    // Estado: E: exitoso, R: rechazado, F: Fallida
    private String estado;

    private String descripcion;
}
