package com.agtech.cloudbedsbatchcr.services;

import com.agtech.cloudbedsbatchcr.pojo.atvadapter.DocumentType;
import com.agtech.cloudbedsbatchcr.pojo.atvadapter.Invoice;
import org.springframework.stereotype.Component;

@Component
public class PendingIntegrationStrategy extends DocumentoFiscalStrategy {

    @Override
    public boolean supports(String status) {
        return "PENDING_INTEGRATION".equalsIgnoreCase(status);
    }

    @Override
    public boolean isCreditNote() {
        return false;
    }

    @Override
    public void aplicarReferencia(Invoice invoice, Long numeroFacturaReferencia, String claveReferencia) {
        // No aplica para facturas normales.
    }

    @Override
    protected DocumentType obtenerTipoDocumento(boolean clienteIdentificado) {
        return clienteIdentificado ? DocumentType.F : DocumentType.T;
    }

    @Override
    protected String obtenerTipoDocClave(DocumentType tipoDocumento) {
        return tipoDocumento == DocumentType.F ? "01" : "04";
    }
}
