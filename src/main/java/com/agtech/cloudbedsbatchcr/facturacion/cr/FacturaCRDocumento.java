package com.agtech.cloudbedsbatchcr.facturacion.cr;

import com.agtech.cloudbedsbatchcr.facturacion.FacturaDocumento;
import com.agtech.cloudbedsbatchcr.pojo.atvadapter.DocumentType;

public class FacturaCRDocumento implements FacturaDocumento {

    @Override
    public DocumentType obtenerTipoDocumento(boolean clienteIdentificado) {
        return clienteIdentificado ? DocumentType.F : DocumentType.T;
    }

    @Override
    public String obtenerTipoDocClave(DocumentType tipoDocumento) {
        return tipoDocumento == DocumentType.F ? "01" : "04";
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

