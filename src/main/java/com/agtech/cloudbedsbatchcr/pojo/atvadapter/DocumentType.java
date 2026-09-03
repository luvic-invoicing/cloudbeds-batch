package com.agtech.cloudbedsbatchcr.pojo.atvadapter;

public enum DocumentType {
    T("Tiquete"),
    F("Factura"),
    NCF("NotaCreditoFactura"),
    NCT("NotaCreditoTiquete"),
    ND("NotaDebito"),
    FEC("FacturaCompra"),
    NCPF("NotaCreditoParcialFactura"),
    NCPT("NotaCreditoParcialTiquete");

    private final String code;

    private DocumentType(String code) {
        this.code = code;
    }

    public String toString() {
        return code;
    }
}
