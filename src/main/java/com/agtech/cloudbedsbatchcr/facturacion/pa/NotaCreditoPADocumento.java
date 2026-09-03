package com.agtech.cloudbedsbatchcr.facturacion.pa;

import com.agtech.cloudbedsbatchcr.facturacion.NotaCreditoDocumento;
import com.agtech.cloudbedsbatchcr.pojo.atvadapter.DocumentType;
import com.agtech.cloudbedsbatchcr.pojo.atvadapter.Invoice;

public class NotaCreditoPADocumento implements NotaCreditoDocumento {

    @Override
    public DocumentType obtenerTipoDocumento(boolean clienteIdentificado) {
        // Placeholder until Panama credit note type mapping is finalized.
        return DocumentType.NCF;
    }

    @Override
    public String obtenerTipoDocClave(DocumentType tipoDocumento) {
        return "03";
    }

    @Override
    public void aplicarReferencia(Invoice invoice, Long numeroFacturaReferencia, String claveReferencia) {
        invoice.setReference(claveReferencia);
        invoice.setOtherText(String.format("Nota de credito a factura %s", numeroFacturaReferencia));
    }
}

