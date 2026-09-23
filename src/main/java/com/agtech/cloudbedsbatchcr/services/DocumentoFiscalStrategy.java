package com.agtech.cloudbedsbatchcr.services;

import com.agtech.cloudbedsbatchcr.entities.CbProperties;
import com.agtech.cloudbedsbatchcr.pojo.atvadapter.*;
import com.agtech.cloudbedsbatchcr.pojo.cloudbeds.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base para construir comprobantes fiscales de Cloudbeds.
 * Centraliza la lógica común y delega a cada subclase el tipo y código fiscal del comprobante.
 */
public abstract class DocumentoFiscalStrategy {

    @Autowired
    private UtilService utilService;

    /** Indica si la estrategia procesa el estado de documento fiscal recibido. */
    public abstract boolean supports(String status);

    /** Indica si el comprobante generado es una nota de crédito. */
    public abstract boolean isCreditNote();

    /** Aplica la referencia de la factura original cuando el comprobante lo requiere. */
    public abstract void aplicarReferencia(Invoice invoice, Long numeroFacturaReferencia, String claveReferencia);

    /** Resuelve el tipo de documento según exista identificación válida del cliente. */
    protected abstract DocumentType obtenerTipoDocumento(boolean clienteIdentificado);

    /** Resuelve el código de tipo de documento requerido para generar la clave fiscal. */
    protected abstract String obtenerTipoDocClave(DocumentType tipoDocumento);

    /** Construye el comprobante con cliente, clave fiscal, pagos, líneas e impuestos. */
    public Invoice construirInvoice(CbProperties cbProperty, CBReservationInfoResponse cbReservation,
                                   CBInvoiceResponse cbInvoiceResponse, String cloudbedsInvoiceId,
                                   CBTaxes cbTaxes, Double exchange) {
        return construirInvoiceCompartida(cbProperty, cbReservation, cbInvoiceResponse, cloudbedsInvoiceId, cbTaxes, exchange);
    }

    /**
     * Implementa el armado común reutilizable por las estrategias concretas.
     * Las subclases pueden invocarlo si requieren extender el comprobante antes o después de construirlo.
     */
    protected Invoice construirInvoiceCompartida(CbProperties cbProperty, CBReservationInfoResponse cbReservation,
                                                  CBInvoiceResponse cbInvoiceResponse, String cloudbedsInvoiceId,
                                                  CBTaxes cbTaxes, Double exchange) {
        Invoice invoice = new Invoice();
        invoice.setCustomerId(cbProperty.getTaxIdentificacion());
        invoice.setCode(cbProperty.getCurrency());
        invoice.setExchange("USD".equals(cbProperty.getCurrency()) ? exchange : 1.00);
        invoice.setSystem(ErpSystem.CLOUD_BEDS_HOTEL);

        CBGuestInfo guest = cbReservation.getData().getGuestList().values().stream().findFirst().orElse(null);
        boolean identifiedCustomer = configurarCliente(invoice, guest);
        invoice.setDocumentType(obtenerTipoDocumento(identifiedCustomer));
        configurarClave(invoice, cbProperty, cloudbedsInvoiceId);
        invoice.setCodeSellCondition("01");

        List<CBInvoiceItem> transactions = cbInvoiceResponse.getData().getItems();
        invoice.setCodesPaid(construirMediosPago(transactions));
        configurarLineas(invoice, transactions, cbProperty, cbTaxes);
        return invoice;
    }

    /** Configura el receptor a partir del primer huésped de la reservación. */
    private boolean configurarCliente(Invoice invoice, CBGuestInfo guest) {
        if (guest == null) {
            return false;
        }
        if (guest.getCompanyTaxID() != null && !guest.getCompanyTaxID().isEmpty()) {
            invoice.setClientIdType("02");
            invoice.setClientId(guest.getCompanyTaxID());
            invoice.setClientName(guest.getCompanyName());
            invoice.setClientEmail(guest.getGuestEmail());
            return true;
        }
        String documentType = guest.getGuestDocumentType() == null ? "" : guest.getGuestDocumentType().toLowerCase();
        if ("dni".equals(documentType)) {
            invoice.setClientIdType("01");
        } else if ("passport".equals(documentType)) {
            invoice.setClientIdType("03");
        } else {
            return false;
        }
        invoice.setClientId(guest.getGuestDocumentNumber());
        invoice.setClientName(String.format("%s %s", guest.getGuestFirstName(), guest.getGuestLastName() == null ? "" : guest.getGuestLastName()).trim());
        invoice.setClientEmail(guest.getGuestEmail());
        return true;
    }

    /** Genera y asigna la clave, consecutivo y punto de venta del comprobante. */
    private void configurarClave(Invoice invoice, CbProperties property, String cloudbedsInvoiceId) {
        int pointOfSale = property.getPosNumber() == null ? 1 : Integer.parseInt(property.getPosNumber());
        int headOffice = property.getHeadOfficeNumber() == null ? 1 : Integer.parseInt(property.getHeadOfficeNumber());
        KeyGeneratorRequest request = new KeyGeneratorRequest();
        request.setTipoDoc(obtenerTipoDocClave(invoice.getDocumentType()));
        request.setPuntoVenta(pointOfSale);
        request.setMatriz(headOffice);
        request.setNumIdentification(property.getTaxIdentificacion());
        request.setProdSequence(true);
        KeyGeneratorResponse keyResponse = utilService.generateKey(request);
        invoice.setPointOfSale(pointOfSale);
        invoice.setSecuencia(keyResponse.getCurrentConsecutiveNumber());
        invoice.setFiscalConsecutive(keyResponse.getConsecutiveNumber());
        invoice.setBillKey(keyResponse.getVoucherKey());
        invoice.setIdErp(cloudbedsInvoiceId);
    }

    /** Mapea cargos e impuestos a líneas fiscales y registra cargos sin impuestos por separado. */
    private void configurarLineas(Invoice invoice, List<CBInvoiceItem> transactions, CbProperties property, CBTaxes taxes) {
        int lineNumber = 1;
        List<InvoiceOtherCharges> otherCharges = new ArrayList<>();
        for (CBInvoiceItem transaction : transactions) {
            if (Double.parseDouble(transaction.getNetAmount()) <= 0 || "payment".equals(transaction.getType())) {
                continue;
            }
            if (transaction.getTaxes() == null || transaction.getTaxes().isEmpty()) {
                otherCharges.add(crearOtroCargo(transaction));
                continue;
            }
            InvoiceLine line = new InvoiceLine();
            line.setLineNumber(lineNumber++);
            line.setProductCode(property.getProductCodeDefault());
            line.setUnidMeasure("rate".equals(transaction.getType()) ? "Al" : "Unid");
            line.setDescription(transaction.getDescription());
            line.setQuantity(1);
            line.setUnitPrice(Double.parseDouble(transaction.getTotalAmount()));
            line.setDiscountAmount(0);
            for (CBInvoiceTax tax : transaction.getTaxes()) {
                if (tax.getAmount() == null || Double.parseDouble(tax.getAmount()) <= 0) {
                    continue;
                }
                double taxAmount = Double.parseDouble(tax.getAmount());
                InvoiceLineTax lineTax = new InvoiceLineTax();
                lineTax.setRate(resolverTasaImpuesto(tax, transaction, taxes, taxAmount));
                lineTax.setRateTaxCode("08");
                lineTax.setTaxCode("01");
                lineTax.setAmount(taxAmount);
                line.getLineTaxes().add(lineTax);
            }
            invoice.getLines().add(line);
        }
        if (!otherCharges.isEmpty()) {
            invoice.setInvoiceOtherCharges(otherCharges);
        }
    }

    /** Calcula la tasa desde la configuración de impuestos o, si no existe, desde los montos de la transacción. */
    private double resolverTasaImpuesto(CBInvoiceTax tax, CBInvoiceItem transaction, CBTaxes taxes, double taxAmount) {
        Optional<CBTaxData> configuredTax = taxes.getData().stream().filter(item -> item.getTaxID().equals(tax.getTaxID())).findFirst();
        if (configuredTax.isPresent() && configuredTax.get().getAmount() != null) {
            return Double.parseDouble(configuredTax.get().getAmount());
        }
        return Math.round((taxAmount / Double.parseDouble(transaction.getTotalAmount())) * 100);
    }

    /** Crea un cargo adicional para transacciones sin impuestos. */
    private InvoiceOtherCharges crearOtroCargo(CBInvoiceItem transaction) {
        InvoiceOtherCharges charge = new InvoiceOtherCharges();
        charge.setDocumentType("99");
        String detail = transaction.getDescription();
        if (detail == null || detail.isEmpty()) {
            detail = "otros cargos";
        }
        if (detail.length() < 5) {
            detail = String.format("%-5s", detail).replace(' ', '*');
        }
        charge.setDetail(detail);
        charge.setChargeAmount(Double.parseDouble(transaction.getTotalAmount()));
        charge.setPercentaje(0);
        return charge;
    }

    /** Configura hasta cuatro medios de pago a partir de las transacciones de tipo payment. */
    private CodesPaid construirMediosPago(List<CBInvoiceItem> transactions) {
        CodesPaid codesPaid = new CodesPaid();
        int paymentCount = 0;
        for (CBInvoiceItem transaction : transactions) {
            if (!"payment".equals(transaction.getType()) || paymentCount == 4) {
                continue;
            }
            String paymentCode = obtenerCodigoMedioPago(transaction.getDescription());
            if (paymentCount == 0) {
                codesPaid.setCodePaidMethod1(paymentCode);
            } else if (paymentCount == 1) {
                codesPaid.setCodePaidMethod2(paymentCode);
            } else if (paymentCount == 2) {
                codesPaid.setCodePaidMethod3(paymentCode);
            } else {
                codesPaid.setCodePaidMethod4(paymentCode);
            }
            paymentCount++;
        }
        if (paymentCount == 0) {
            codesPaid.setCodePaidMethod1("01");
        }
        return codesPaid;
    }

    /** Traduce la descripción de Cloudbeds al código de medio de pago requerido por Hacienda. */
    private String obtenerCodigoMedioPago(String descripcionMedioPago) {
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

