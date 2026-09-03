package com.agtech.cloudbedsbatchcr.facturacion.cr;

import com.agtech.cloudbedsbatchcr.facturacion.FacturaDocumento;
import com.agtech.cloudbedsbatchcr.facturacion.FacturaTransmisor;
import com.agtech.cloudbedsbatchcr.facturacion.FacturacionAbstractFactory;
import com.agtech.cloudbedsbatchcr.facturacion.NotaCreditoDocumento;

public class CostaRicaFacturacionFactory implements FacturacionAbstractFactory {
    @Override
    public FacturaDocumento crearFactura() {
        return new FacturaCRDocumento();
    }

    @Override
    public NotaCreditoDocumento crearNotaCredito() {
        return new NotaCreditoCRDocumento();
    }

    @Override
    public FacturaTransmisor crearTransmisor() {
        return new TransmisorCRREST();
    }
}

