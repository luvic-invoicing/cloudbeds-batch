package com.agtech.cloudbedsbatchcr.facturacion.pa;

import com.agtech.cloudbedsbatchcr.facturacion.FacturaDocumento;
import com.agtech.cloudbedsbatchcr.facturacion.FacturaTransmisor;
import com.agtech.cloudbedsbatchcr.facturacion.FacturacionAbstractFactory;
import com.agtech.cloudbedsbatchcr.facturacion.NotaCreditoDocumento;

public class PanamaFacturacionFactory implements FacturacionAbstractFactory {
    @Override
    public FacturaDocumento crearFactura() {
        return new FacturaPADocumento();
    }

    @Override
    public NotaCreditoDocumento crearNotaCredito() {
        return new NotaCreditoPADocumento();
    }

    @Override
    public FacturaTransmisor crearTransmisor() {
        return new TransmisorPAREST();
    }
}

