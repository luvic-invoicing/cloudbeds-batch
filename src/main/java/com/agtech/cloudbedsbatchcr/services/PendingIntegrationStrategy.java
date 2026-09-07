package com.agtech.cloudbedsbatchcr.services;

import com.agtech.cloudbedsbatchcr.entities.CbProperties;
import com.agtech.cloudbedsbatchcr.pojo.atvadapter.*;
import com.agtech.cloudbedsbatchcr.pojo.cloudbeds.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PendingIntegrationStrategy implements DocumentoFiscalStrategy {

    private static final Logger logger = LogManager.getLogger(PendingIntegrationStrategy.class);

    @Autowired
    private UtilService utilService;

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
    public Invoice construirInvoice(
            CbProperties cbProperty,
            CBReservationInfoResponse cbReservation,
            CBInvoiceResponse cbInvoiceResponse,
            String cloudbedsInvoiceId,
            CBTaxes cbTaxes,
            Double exchange
    ) throws Exception {
        Invoice invoice = new Invoice();
        invoice.setCustomerId(cbProperty.getTaxIdentificacion());
        invoice.setCode(cbProperty.getCurrency());
        invoice.setExchange("USD".equals(cbProperty.getCurrency()) ? exchange : 1.00);
        invoice.setSystem(ErpSystem.CLOUD_BEDS_HOTEL);

        // Obtener información del cliente
        CBGuestInfo cbGuest = cbReservation.getData().getGuestList()
                .values()
                .stream()
                .findFirst()
                .orElse(null);

        boolean identifiedCustomer = false;
        if (cbGuest != null) {
            String taxCompanyId = cbGuest.getCompanyTaxID();
            String documentType = cbGuest.getGuestDocumentType() != null ? cbGuest.getGuestDocumentType().toLowerCase() : "";

            if (taxCompanyId != null && !taxCompanyId.isEmpty()) {
                invoice.setClientIdType("02");
                invoice.setClientId(taxCompanyId);
                invoice.setClientName(cbGuest.getCompanyName());
                invoice.setClientEmail(cbGuest.getGuestEmail());
                identifiedCustomer = true;
            } else if (!documentType.isEmpty()) {
                boolean enabledId = false;
                if (documentType.equals("dni")) {
                    invoice.setClientIdType("01");
                    enabledId = true;
                } else if (documentType.equals("passport")) {
                    invoice.setClientIdType("03");
                    enabledId = true;
                }

                if (enabledId) {
                    invoice.setClientId(cbGuest.getGuestDocumentNumber());
                    invoice.setClientName(String.format("%s %s", cbGuest.getGuestFirstName(), cbGuest.getGuestLastName() != null ? cbGuest.getGuestLastName() : "").trim());
                    invoice.setClientEmail(cbGuest.getGuestEmail());
                    identifiedCustomer = true;
                }
            }

            logger.info(String.format("El tipo de documento registrado para el huesped %s es %s", cbGuest.getGuestFirstName(), cbGuest.getGuestDocumentType()));
        }

        invoice.setDocumentType(obtenerTipoDocumento(identifiedCustomer));

        // Generar clave y datos de factura
        KeyGeneratorRequest request = new KeyGeneratorRequest();
        request.setTipoDoc(obtenerTipoDocClave(invoice.getDocumentType()));
        if (cbProperty.getPosNumber() != null) {
            request.setPuntoVenta(Integer.parseInt(cbProperty.getPosNumber()));
            request.setMatriz(Integer.parseInt(cbProperty.getHeadOfficeNumber()));
            invoice.setPointOfSale(Integer.parseInt(cbProperty.getPosNumber()));
        } else {
            request.setPuntoVenta(1);
            request.setMatriz(1);
            invoice.setPointOfSale(1);
        }
        request.setNumIdentification(cbProperty.getTaxIdentificacion());
        request.setProdSequence(true);

        KeyGeneratorResponse keyResponse = utilService.generateKey(request);
        invoice.setSecuencia(keyResponse.getCurrentConsecutiveNumber());
        invoice.setFiscalConsecutive(keyResponse.getConsecutiveNumber());
        invoice.setBillKey(keyResponse.getVoucherKey());
        invoice.setIdErp(cloudbedsInvoiceId);

        invoice.setCodeSellCondition("01");

        // Procesar items y pagos
        List<CBInvoiceItem> transactions = cbInvoiceResponse.getData().getItems();
        List<CBInvoiceItem> paymentsList = transactions.stream()
                .filter(t -> "payment".equals(t.getType()))
                .collect(Collectors.toList());

        CodesPaid codesPaid = new CodesPaid();
        if (!paymentsList.isEmpty()) {
            int contador = 1;
            for (CBInvoiceItem paymentItem : paymentsList) {
                String codePaid = obtenerCodigoMedioPago(paymentItem.getDescription());
                if (contador == 1) {
                    codesPaid.setCodePaidMethod1(codePaid);
                } else if (contador == 2) {
                    codesPaid.setCodePaidMethod2(codePaid);
                } else if (contador == 3) {
                    codesPaid.setCodePaidMethod3(codePaid);
                } else if (contador == 4) {
                    codesPaid.setCodePaidMethod4(codePaid);
                }
                contador++;
            }
        } else {
            codesPaid.setCodePaidMethod1("01");
        }
        invoice.setCodesPaid(codesPaid);

        // Procesar líneas e impuestos
        Integer lineNumber = 1;
        List<InvoiceOtherCharges> otherChargesList = new ArrayList<>();
        for (CBInvoiceItem transaction : transactions) {
            if (Double.parseDouble(transaction.getNetAmount()) > 0 && !"payment".equals(transaction.getType())) {
                if (transaction.getTaxes() != null && !transaction.getTaxes().isEmpty()) {
                    InvoiceLine invoiceLine = new InvoiceLine();
                    invoiceLine.setLineNumber(lineNumber++);
                    invoiceLine.setProductCode(cbProperty.getProductCodeDefault());
                    invoiceLine.setUnidMeasure("rate".equals(transaction.getType()) ? "Al" : "Unid");
                    invoiceLine.setDescription(transaction.getDescription());
                    invoiceLine.setQuantity(1);
                    invoiceLine.setUnitPrice(Double.parseDouble(transaction.getTotalAmount()));
                    invoiceLine.setDiscountAmount(0);

                    for (CBInvoiceTax tax : transaction.getTaxes()) {
                        if (tax.getAmount() != null && Double.parseDouble(tax.getAmount()) > 0) {
                            double valorImpuesto = Double.parseDouble(tax.getAmount());
                            double percentajeReservation = 0.00;
                            try {
                                Optional<CBTaxData> taxOptional = cbTaxes.getData().stream()
                                        .filter(t -> t.getTaxID().equals(tax.getTaxID()))
                                        .findFirst();
                                if (taxOptional.isPresent() && taxOptional.get().getAmount() != null) {
                                    percentajeReservation = Double.parseDouble(taxOptional.get().getAmount());
                                } else {
                                    percentajeReservation = Math.round((valorImpuesto / Double.parseDouble(transaction.getTotalAmount())) * 100);
                                }
                            } catch (Exception ex) {
                                logger.error("Error al obtener la informacion del impuesto", ex);
                                percentajeReservation = Math.round((Double.parseDouble(tax.getAmount()) / Double.parseDouble(transaction.getTotalAmount())) * 100);
                            }

                            InvoiceLineTax invoiceLineTax = new InvoiceLineTax();
                            invoiceLineTax.setRate(percentajeReservation);
                            invoiceLineTax.setRateTaxCode("08");
                            invoiceLineTax.setTaxCode("01");
                            invoiceLineTax.setAmount(valorImpuesto);
                            invoiceLine.getLineTaxes().add(invoiceLineTax);
                        }
                    }
                    invoice.getLines().add(invoiceLine);
                } else {
                    InvoiceOtherCharges otherCharge = new InvoiceOtherCharges();
                    otherCharge.setDocumentType("99");
                    String detail = transaction.getDescription();
                    if (detail == null || detail.isEmpty()) {
                        detail = "otros cargos";
                    }
                    if (detail.length() < 5) {
                        detail = String.format("%-5s", detail).replace(' ', '*');
                    }
                    otherCharge.setDetail(detail);
                    otherCharge.setChargeAmount(Double.parseDouble(transaction.getTotalAmount()));
                    otherCharge.setPercentaje(0);
                    otherChargesList.add(otherCharge);
                }
            }
        }

        if (!otherChargesList.isEmpty()) {
            invoice.setInvoiceOtherCharges(otherChargesList);
        }

        return invoice;
    }

    public String obtenerTipoDocClave(DocumentType tipoDocumento) {
        return tipoDocumento == DocumentType.F ? "01" : "04";
    }

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

    public DocumentType obtenerTipoDocumento(boolean clienteIdentificado) {
        return clienteIdentificado ? DocumentType.F : DocumentType.T;
    }

}

