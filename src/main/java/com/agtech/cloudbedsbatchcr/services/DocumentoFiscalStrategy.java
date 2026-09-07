package com.agtech.cloudbedsbatchcr.services;

import com.agtech.cloudbedsbatchcr.entities.CbProperties;
import com.agtech.cloudbedsbatchcr.pojo.atvadapter.Invoice;
import com.agtech.cloudbedsbatchcr.pojo.cloudbeds.CBInvoiceResponse;
import com.agtech.cloudbedsbatchcr.pojo.cloudbeds.CBReservationInfoResponse;
import com.agtech.cloudbedsbatchcr.pojo.cloudbeds.CBTaxes;

public interface DocumentoFiscalStrategy {
    boolean supports(String status);

    boolean isCreditNote();

    void aplicarReferencia(Invoice invoice, Long numeroFacturaReferencia, String claveReferencia);

    Invoice construirInvoice(CbProperties cbProperty, CBReservationInfoResponse cbReservation, CBInvoiceResponse cbInvoiceResponse,
                             String cloudbedsInvoiceId, CBTaxes cbTaxes, Double exchange) throws Exception;
}

