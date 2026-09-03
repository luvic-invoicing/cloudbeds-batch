package com.agtech.cloudbedsbatchcr.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TipoCambio {
    @JsonProperty("venta")
    private DetalleTipoCambio venta;

    @JsonProperty("compra")
    private DetalleTipoCambio compra;

    // Getters y setters

    public DetalleTipoCambio getVenta() {
        return venta;
    }

    public void setVenta(DetalleTipoCambio venta) {
        this.venta = venta;
    }

    public DetalleTipoCambio getCompra() {
        return compra;
    }

    public void setCompra(DetalleTipoCambio compra) {
        this.compra = compra;
    }
}