package com.agtech.cloudbedsbatchcr.facturacion;

import com.agtech.cloudbedsbatchcr.pojo.atvadapter.DocumentType;
import com.agtech.cloudbedsbatchcr.pojo.atvadapter.Invoice;

public interface NotaCreditoDocumento {
    DocumentType obtenerTipoDocumento(boolean clienteIdentificado);
    String obtenerTipoDocClave(DocumentType tipoDocumento);
    void aplicarReferencia(Invoice invoice, Long numeroFacturaReferencia, String claveReferencia);
}

