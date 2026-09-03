package com.agtech.cloudbedsbatchcr.facturacion;

import com.agtech.cloudbedsbatchcr.pojo.atvadapter.DocumentType;

public interface FacturaDocumento {
    DocumentType obtenerTipoDocumento(boolean clienteIdentificado);
    String obtenerTipoDocClave(DocumentType tipoDocumento);
    String obtenerCodigoMedioPago(String descripcionMedioPago);
}

