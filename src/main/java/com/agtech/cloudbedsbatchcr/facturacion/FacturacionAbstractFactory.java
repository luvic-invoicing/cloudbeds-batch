package com.agtech.cloudbedsbatchcr.facturacion;

public interface FacturacionAbstractFactory {
    FacturaDocumento crearFactura();
    NotaCreditoDocumento crearNotaCredito();
    FacturaTransmisor crearTransmisor();
}

