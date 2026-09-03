package com.agtech.cloudbedsbatchcr.facturacion.pa;

import com.agtech.cloudbedsbatchcr.facturacion.FacturaDocumento;
import com.agtech.cloudbedsbatchcr.pojo.atvadapter.DocumentType;

public class FacturaPADocumento implements FacturaDocumento {

    @Override
    public DocumentType obtenerTipoDocumento(boolean clienteIdentificado) {
        // Placeholder until Panama fiscal document types are finalized.
        return DocumentType.F;
    }

    @Override
    public String obtenerTipoDocClave(DocumentType tipoDocumento) {
        // Placeholder mapping aligned with adapter expectations.
        return "01";
    }

    @Override
    public String obtenerCodigoMedioPago(String descripcionMedioPago) {
        if (descripcionMedioPago == null) {
            return "01";
        }

        switch (descripcionMedioPago.toLowerCase()) {
            case "credit card":
            case "tarjeta de credito":
            case "check":
            case "tarjeta de debito":
                return "02";
            case "cash":
            case "efectivo":
                return "01";
            case "bank transfer":
            case "transferencia bancaria":
                return "04";
            default:
                return "01";
        }
    }
}

