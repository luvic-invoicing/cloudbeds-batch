package com.agtech.cloudbedsbatchcr.services;

import com.agtech.cloudbedsbatchcr.pojo.atvadapter.DocumentType;
import com.agtech.cloudbedsbatchcr.pojo.atvadapter.Invoice;
import org.springframework.stereotype.Component;

@Component
public class CancelRequestedStrategy extends DocumentoFiscalStrategy {

    @Override
    public boolean supports(String status) {
        return "CANCEL_REQUESTED".equalsIgnoreCase(status);
    }

    @Override
    public boolean isCreditNote() {
        return true;
    }

    @Override
    public void aplicarReferencia(Invoice invoice, Long numeroFacturaReferencia, String claveReferencia) {
        invoice.setReference(claveReferencia);
        invoice.setOtherText(String.format("Nota de credito a factura %s", numeroFacturaReferencia));
    }

    @Override
    protected DocumentType obtenerTipoDocumento(boolean clienteIdentificado) {
        return clienteIdentificado ? DocumentType.NCF : DocumentType.NCT;
    }

    @Override
    protected String obtenerTipoDocClave(DocumentType tipoDocumento) {
        return "03";
    }
}
