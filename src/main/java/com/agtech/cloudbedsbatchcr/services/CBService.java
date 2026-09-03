package com.agtech.cloudbedsbatchcr.services;

import com.agtech.cloudbedsbatchcr.entities.*;
import com.agtech.cloudbedsbatchcr.exceptions.DocumentoFiscalNotFoundException;
import com.agtech.cloudbedsbatchcr.exceptions.PropertyNotFoundException;
import com.agtech.cloudbedsbatchcr.facturacion.*;
import com.agtech.cloudbedsbatchcr.pojo.TipoCambio;
import com.agtech.cloudbedsbatchcr.pojo.atvadapter.*;
import com.agtech.cloudbedsbatchcr.pojo.cloudbeds.*;
import com.agtech.cloudbedsbatchcr.pojo.webhook.*;
import com.agtech.cloudbedsbatchcr.repositories.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestTemplate;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CBService {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Logger logger = LogManager.getLogger(CBService.class);

    @Autowired
    private Environment env;

    @Autowired
    private ICbAccountRepository cbAccountRepository;

    @Autowired
    private ICbPropertiesRepository propertiesRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentTypeRepository paymentTypeRepository;

    @Autowired
    private UtilService utilService;

    @Autowired
    private ICbInvoiceRepository cbInvoiceRepository;

    @Autowired
    private  TaxService taxService;

    @Autowired
    private FacturacionFactoryProvider facturacionFactoryProvider;

    public PropertyResponse createProperty(Property property) throws Exception {
        try{
            logger.info(String.format("Se solicita crear la propiedad con identificación %s e id de propiedad", property.getTaxIdentification(), property.getPropertyId()));

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent", "Mozilla/5.0");
            headers.add("Authorization", String.format("Bearer %s", property.getApiKey()));

            // Crear la solicitud HTTP con el cuerpo del formulario y los encabezados
            HttpEntity requestEntity = new HttpEntity<>(headers);

            // Enviar la solicitud POST y recibir la respuesta
            ResponseEntity<CBProperty> responseEntity = restTemplate.exchange(
                    String.format("%s?propertyID=%s", "https://api.cloudbeds.com/api/v1.2/getHotelDetails", property.getPropertyId()),
                    HttpMethod.GET,
                    requestEntity,
                    CBProperty.class);

            if(responseEntity.getBody().getSuccess()){
                ObjectMapper mapper = new ObjectMapper();
                logger.info(String.format("Datos de la propiedad con identificación %s consultados correctamente -> %s", property.getTaxIdentification(), mapper.writeValueAsString(responseEntity.getBody())));

                //Se registra los datos de la nueva propiedad:
                CbAccount account = new CbAccount();
                account.setApiUrl("https://api.cloudbeds.com/api/v1.2");
                account.setApiAccountingUrl("https://api.cloudbeds.com/accounting/v1.0");
                account.setEnabled(true);
                account.setExchange(490.00);
                account.setTcIndicator("V");
                account.setApiKey(property.getApiKey());
                account = cbAccountRepository.save(account);

                CbProperties properties = new CbProperties();
                properties.setPropertyId(Long.valueOf(property.getPropertyId()));
                properties.setCbAccount(account);
                properties.setEnable(true);
                properties.setCurrency(responseEntity.getBody().getData().getPropertyCurrency().getCurrencyCode());
                properties.setCountryCode(resolveCountryCode(responseEntity.getBody().getData().getPropertyAddress()));
                properties.setHeadOfficeNumber("1");
                properties.setPosNumber("1");
                properties.setTaxIdentificacion(property.getTaxIdentification());
                properties.setCompanyName(responseEntity.getBody().getData().getPropertyName());
                properties.setProductCodeDefault("6311100000000");
                propertiesRepository.save(properties);

                PropertyResponse response = new PropertyResponse();
                response.setCurrency(responseEntity.getBody().getData().getPropertyCurrency().getCurrencyCode());
                response.setName(responseEntity.getBody().getData().getPropertyName());
                response.setTaxIdentification(property.getTaxIdentification());

                //Se registran únicamente los webhooks del nuevo flujo fiscal documents:
                //1. Cambio de estado de la integracion:
                createWebHook("http://144.202.35.15:8050/api/wh/v1/changeAppState",
                        "integration",
                        "appstate_changed",
                        property.getApiKey(),
                        property.getPropertyId());

                //2. Fiscal documents webhook create
                createWebHook("http://144.202.35.15:8050/api/wh/v1/invoice",
                        "fiscal_document",
                        "create",
                        property.getApiKey(),
                        property.getPropertyId());

                //3. Fiscal documents webhook update
                createWebHook("http://144.202.35.15:8050/api/wh/v1/invoice",
                        "fiscal_document",
                        "update",
                        property.getApiKey(),
                        property.getPropertyId());

                return response;
            }else{
                logger.info(String.format("Se presentó un problema al consultar los datos de la propiedad con identificación %s", property.getTaxIdentification()));
                throw new Exception(String.format("Se presentó un problema al consultar los datos de la propiedad con identificación %s", property.getTaxIdentification()));
            }
        }catch (Exception ex){
            ex.printStackTrace();
            logger.error(String.format("Se presentó un error al crear el cliente con identificación %s", property.getTaxIdentification()), ex);
            throw ex;
        }
    }

    private void createWebHook(String endpointUrl, String object, String action, String apiKey, String propertyID){
        try{

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", String.format("Bearer %s", apiKey));

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("endpointUrl", endpointUrl);
            map.add("object", object);
            map.add("action", action);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(map, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<CBWebhookResponse> responseEntity = restTemplate.exchange(
                    String.format("%s?propertyID=%s", "https://api.cloudbeds.com/api/v1.2/postWebhook", propertyID),
                    HttpMethod.POST,
                    requestEntity,
                    CBWebhookResponse.class
            );

            CBWebhookResponse response = responseEntity.getBody();
            if(response.getSuccess()){
                logger.info(String.format("Webhook registrado correctamente: %s", response.getData().getSubscriptionID()));
            }else{
                logger.error(String.format("Se presentó un problema al crear el webhook con los siguiente parametro: %s -> %s -> %s",
                        endpointUrl,
                        object,
                        action
                ));
            }

        }catch (Exception ex){
            ex.printStackTrace();
            logger.error(
                    String.format("Se presentó un problemas al crear el webhook con los siguiente parametro: %s -> %s -> %s",
                            endpointUrl,
                            object,
                            action
                    ),
                    ex
            );
        }
    }

    private String resolveCountryCode(CBPropertyAddress propertyAddress) {
        if (propertyAddress == null || propertyAddress.getPropertyCountry() == null) {
            return "CR";
        }

        String country = propertyAddress.getPropertyCountry().trim().toUpperCase(Locale.ROOT);
        if ("PA".equals(country) || country.contains("PANAMA")) {
            return "PA";
        }
        if ("CR".equals(country) || country.contains("COSTA RICA")) {
            return "CR";
        }
        return "CR";
    }

    public void changeAppState(AppState appState){

        try {
            ObjectMapper mapper = new ObjectMapper();
            logger.info(String.format("JSON recibido %s", mapper.writeValueAsString(appState)));

             Optional<CbProperties> property = propertiesRepository.findById(appState.getPropertyID());

             if(!property.isPresent()){
                 logger.info(String.format("La propiedad %s no esta parametrizada en el sistema", appState.getPropertyID()));
                 return;
             }

             if(appState.getNewState().equals("enabled")){
                 property.get().setEnable(true);
                 logger.info(String.format("Se activa la propiedad %s (%s)", property.get().getPropertyId(), property.get().getCompanyName()));
             }else{
                 property.get().setEnable(false);
                 logger.info(String.format("Se inactiva la propiedad %s (%s)", property.get().getPropertyId(), property.get().getCompanyName()));
             }
            propertiesRepository.save(property.get());
        }catch (Exception ex){
            ex.printStackTrace();
            logger.error(String.format("Excepción al cambiar el estado de la App de la propiedad %s del estado %s al %s", appState.getPropertyID(), appState.getOldState(), appState.getNewState()));
        }
    }

    public void reservationNote(CbProperties cbProperties, String reservationId, String reservationNoteText) {
        try{
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent", "Mozilla/5.0");
            headers.add("x-api-key", cbProperties.getCbAccount().getApiKey());

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("reservationID", reservationId);
            map.add("reservationNote", reservationNoteText);

            // Crear la solicitud HTTP con el cuerpo del formulario y los encabezados
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(map, headers);

            // Enviar la solicitud POST y recibir la respuesta
            ResponseEntity<CBNoteResponse> responseEntity = restTemplate.exchange(
                    String.format("%s/postReservationNote?propertyID=%s", cbProperties.getCbAccount().getApiUrl(), cbProperties.getPropertyId().toString()),
                    HttpMethod.POST,
                    requestEntity,
                    CBNoteResponse.class);

            CBNoteResponse cbResponseBody = responseEntity != null ? responseEntity.getBody() : null;
            if (cbResponseBody != null) {
                ObjectMapper mapper = new ObjectMapper();
                logger.info(String.format("Respuesta al agregar nota en la reservación %s: %s", reservationId, mapper.writeValueAsString(cbResponseBody)));

                if (cbResponseBody.isSuccess()) {
                    logger.info(String.format("Nota en la reservación %s actualizada correctamente: %s", reservationId, cbResponseBody.getReservationNoteID()));
                } else {
                    logger.info(String.format("Se presentó un problema al agregar la nota en la reservación %s", reservationId));
                }
            } else {
                logger.warn(String.format("Respuesta nula al intentar actualizar la nota de la reservación %s", reservationId));
            }
        }catch (Exception ex){
            ex.printStackTrace();
            logger.error(String.format("Se presentó una excepción al guardar la nota de la reservación %s", reservationId), ex);
        }
    }

    /**
     * Obtiene la información principal de la reservación desde Cloudbeds
     */
    public CBReservationInfoResponse getReservationInformation(CbProperties property, String reservationId) throws Exception {
        try {
            String url = String.format("%s/getReservation?reservationID=%s&propertyID=%s", property.getCbAccount().getApiUrl(), reservationId, property.getPropertyId().toString());

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent", "Mozilla/5.0");
            headers.add("Authorization", String.format("Bearer %s", property.getCbAccount().getApiKey()));
            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<CBReservationInfoResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, CBReservationInfoResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                if (response.getBody().isSuccess()) {
                    logger.info(String.format("Se obtuvo la información principal de la reservación %s correctamente", reservationId));
                    return response.getBody();
                } else {
                    logger.error(String.format("La respuesta de la reservación %s no fue exitosa", reservationId));
                    throw new Exception("La respuesta de la reservación no fue exitosa");
                }
            } else {
                logger.error(String.format("Error al obtener información principal de la reservación %s: %s", reservationId, response.getStatusCode().getReasonPhrase()));
                throw new Exception(response.getStatusCode().getReasonPhrase());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex);
            throw ex;
        }
    }

    private Double getExchange(CbAccount account){
        try{
            LocalDate fechaActual = LocalDate.now();
            logger.info(String.format("Se compara la fecha actual %s con la ultima fecha guardada %s", fechaActual, account.getLastExchangeDate()));
            if(!account.getLastExchangeDate().equals(fechaActual)) {
                //https://api.hacienda.go.cr/indicadores/tc/dolar
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36");
                HttpEntity<String> entity = new HttpEntity<>(headers);
                String url = "https://api.hacienda.go.cr/indicadores/tc/dolar";

                //TipoCambio tipoCambio = restTemplate.getForObject(url, TipoCambio.class);

                ResponseEntity<TipoCambio> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        TipoCambio.class
                );

                TipoCambio tipoCambio = response.getBody();

                logger.info(String.format("Se obtiene el tipo de cambio de compra %s y venta %s", tipoCambio.getCompra().getValor(), tipoCambio.getVenta().getValor()));
                Double tipoCambioBCCR = tipoCambio.getCompra().getValor();
                if(account.getTcIndicator() != null && account.getTcIndicator().equals("V")){
                    tipoCambioBCCR = tipoCambio.getVenta().getValor();
                }
                account.setExchange(tipoCambioBCCR);
                account.setLastExchangeDate(new Date());
                cbAccountRepository.save(account);
                return tipoCambioBCCR;
            }else{
                logger.info(String.format("Se usa el ultimo tipo de cambio registrado %s", account.getExchange()));
                return account.getExchange();
            }
        }catch (Exception ex){
            logger.error(ex);
            return account.getExchange();
        }
    }

    @Transactional
    public void invoice(InvoiceRequest invoiceRequest){

        try{
            String jsonRecibido = MAPPER.writeValueAsString(invoiceRequest);

            logger.info(String.format("JSON recibido %s", jsonRecibido));

            final Long propertyId = resolvePropertyId(invoiceRequest)
                    .orElseThrow(() -> new PropertyNotFoundException( "La propertyId no puede ser encontrada"));

            final String fiscalDocumentId = invoiceRequest.getId();
            final CbProperties cbProperty = getCbProperties(propertyId);

            final FiscalDocumentContext fiscalDocument = getFiscalDocumentContext(fiscalDocumentId, propertyId, cbProperty.getCbAccount().getApiKey())
                    .orElseThrow(() -> new DocumentoFiscalNotFoundException(String.format("No existe documento fiscal para id %s", invoiceRequest.getId())));


            FacturacionAbstractFactory facturacionFactory = facturacionFactoryProvider.obtenerFactory(cbProperty.getCountryCode());
            FacturaDocumento facturaDocumento = facturacionFactory.crearFactura();
            NotaCreditoDocumento notaCreditoDocumento = facturacionFactory.crearNotaCredito();
            FacturaTransmisor facturaTransmisor = facturacionFactory.crearTransmisor();

            logger.info(MAPPER.writeValueAsString(fiscalDocument.getInvoiceDetail()));

            final String reservationId = fiscalDocument.getReservationId();
            final String cloudbedsInvoiceId = fiscalDocument.getInvoiceDetail().getData().getInvoiceID();

            final boolean creditNote = isCreditNote(invoiceRequest.getStatus(), cloudbedsInvoiceId, reservationId);

            final Optional<CbInvoice> invoiceFound = invoiceAlreadyExists(cloudbedsInvoiceId, reservationId, propertyId, creditNote);

            if(invoiceFound.isPresent()){
                logger.warn(String.format("Ya existe la %s %s de la reservacion %s registrada en el sistema", (creditNote ? "Nota de crédito" : "Factura"), cloudbedsInvoiceId, reservationId));
                return;
            }
            final CbInvoiceId newInvoiceId = invoiceFound.get().getInvoiceId();
            Optional<CbInvoice> originalInvoice = null;
            String  claveReferencia = null;
            if(creditNote){
                //Se busca la factura original para validar que exista y no este anulada
                CbInvoiceId originalInvoiceId = new CbInvoiceId();
                originalInvoiceId.setInvoiceId(cloudbedsInvoiceId);
                originalInvoiceId.setReservationId(reservationId);
                originalInvoiceId.setPropertyId(propertyId);
                originalInvoiceId.setType(CbTypeInvoice.INVOICE);

                originalInvoice = cbInvoiceRepository.findById(originalInvoiceId);

                if(!originalInvoice.isPresent()){
                    logger.warn(String.format("La factura %s de la reservacion %s no existe para ser anulada en el sistema", cloudbedsInvoiceId, fiscalDocument.getInvoiceDetail().getData().getReservationID()));
                    return;
                } else if(originalInvoice.isPresent() && originalInvoice.get().getState().equals(CbStateInvoice.VOIDED)){
                    logger.warn(String.format("La factura %s de la reservacion %s ya se encuentra anulada", cloudbedsInvoiceId, fiscalDocument.getInvoiceDetail().getData().getReservationID()));
                    return;
                }else{
                    claveReferencia = originalInvoice.get().getClaveHacienda();
                }
            }

            //Se crea la entidad reservation para guardar los datos
            CbInvoice cbInvoice = new CbInvoice();
            cbInvoice.setDate(new Date());
            cbInvoice.setInvoiceId(newInvoiceId);
            cbInvoice.setProperties(cbProperty);
            cbInvoiceRepository.save(cbInvoice);

            //Se obtienen los impuestos parametrizados:
            final CBTaxes cbTaxes = taxService.getTaxes(cbProperty, cbProperty.getCbAccount().getApiKey());

            logger.info(MAPPER.writeValueAsString(cbTaxes));

            //Se obtiene la información de la reservación:
            final CBReservationInfoResponse cbReservation = getReservationInformation(cbProperty, newInvoiceId.getReservationId());

            cbInvoice.setJsonDataReservation(MAPPER.writeValueAsString(cbReservation));
            cbInvoice.setJsonDataInvoice(MAPPER.writeValueAsString(fiscalDocument.getInvoiceDetail()));
            cbInvoice.setConsecutiveInvoice(fiscalDocument.getInvoiceDetail().getData().getNumber());
            cbInvoice.setJsonRequestCb(jsonRecibido);
            cbInvoice.setOthersMessage(fiscalDocumentId);

            Invoice invoice = new Invoice();
            invoice.setCustomerId(cbProperty.getTaxIdentificacion());
            invoice.setCode(cbProperty.getCurrency());
            if(cbProperty.getCurrency().equals("USD")){
                invoice.setExchange(getExchange(cbProperty.getCbAccount()));
            }else{
                invoice.setExchange(1.00);
            }
            invoice.setSystem(ErpSystem.CLOUD_BEDS_HOTEL);

            invoice.setDocumentType(
                    creditNote
                            ? notaCreditoDocumento.obtenerTipoDocumento(false) : facturaDocumento.obtenerTipoDocumento(false)
            );

            //Se obtiene la infomación del cliente.
            CBGuestInfo cbGuest = cbReservation.getData().getGuestList()
                    .values()
                    .stream()
                    .findFirst()
                    .orElse(null);
            logger.info(String.format("El tipo de documento registrado para el huesped %s en la reservación %s es %s", cbGuest.getGuestFirstName(), fiscalDocument.getInvoiceDetail().getData().getReservationID(), cbGuest.getGuestDocumentType()));

            String taxCompanyId = cbGuest.getCompanyTaxID();
            String documentType = cbGuest.getGuestDocumentType() != null ? cbGuest.getGuestDocumentType().toLowerCase() : "";
            boolean identifiedCustomer = false;

            //Primero se valida que sea una empresa:
            if (taxCompanyId != null && !taxCompanyId.isEmpty()) {
                try {

                    invoice.setClientIdType("02");
                    invoice.setClientId(taxCompanyId);
                    invoice.setClientName(cbGuest.getCompanyName());
                    invoice.setClientEmail(cbGuest.getGuestEmail());
                    identifiedCustomer = true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    logger.error(ex);
                }
            }else if (documentType != null && !documentType.isEmpty()){
                try {
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
                } catch (Exception ex) {
                    ex.printStackTrace();
                    logger.error(String.format("Se presentó un problema al procesar el cliente %s de la factura %s en la empresa %s", cbGuest.getGuestFirstName(), fiscalDocument.getInvoiceDetail().getData().getReservationID(), cbProperty.getCompanyName()));
                    logger.error(ex);
                }
            }

            invoice.setDocumentType(
                    creditNote
                            ? notaCreditoDocumento.obtenerTipoDocumento(identifiedCustomer)
                            : facturaDocumento.obtenerTipoDocumento(identifiedCustomer)
            );

            KeyGeneratorRequest request = new KeyGeneratorRequest();
            request.setTipoDoc(
                    creditNote
                            ? notaCreditoDocumento.obtenerTipoDocClave(invoice.getDocumentType())
                            : facturaDocumento.obtenerTipoDocClave(invoice.getDocumentType())
            ); //Tipo de documento.
            if(cbProperty.getPosNumber() != null){
                request.setPuntoVenta(Integer.parseInt(cbProperty.getPosNumber()));
                request.setMatriz(Integer.parseInt(cbProperty.getHeadOfficeNumber()));
                invoice.setPointOfSale(Integer.parseInt(cbProperty.getPosNumber()));
            }else{
                request.setPuntoVenta(1);
                request.setMatriz(1);
                invoice.setPointOfSale(1);
            }
            request.setNumIdentification(cbProperty.getTaxIdentificacion());
            request.setProdSequence(true);
            KeyGeneratorResponse response = utilService.generateKey(request);
            invoice.setSecuencia(response.getCurrentConsecutiveNumber());
            invoice.setFiscalConsecutive(response.getConsecutiveNumber());
            invoice.setBillKey(response.getVoucherKey());
            invoice.setIdErp(cloudbedsInvoiceId);
            cbInvoice.setClaveHacienda(response.getVoucherKey());
            cbInvoice.setConsecutivoHacienda(response.getConsecutiveNumber());
            cbInvoice.setState(CbStateInvoice.PROGRESS);

            if(creditNote) {
                notaCreditoDocumento.aplicarReferencia(invoice, fiscalDocument.getInvoiceDetail().getData().getNumber(), claveReferencia);
            }

            logger.info(String.format("Se crea la clave %s en la reservación %s", response.getVoucherKey(), fiscalDocument.getInvoiceDetail().getData().getReservationID()));

            invoice.setCodeSellCondition("01");

            List<CBInvoiceItem> transactions = fiscalDocument.getInvoiceDetail().getData().getItems();

            List<CBInvoiceItem> paymentsList = transactions.stream()
                    .filter(t -> t.getType().equals("payment"))
                    .collect(Collectors.toList());

            //Medios de pago.
            String codePaid = "01";
            CodesPaid codesPaid = new CodesPaid();
            if (paymentsList.size() > 0) {
                int contador = 1;
                for (CBInvoiceItem paymentItem : paymentsList) {
                    codePaid = facturaDocumento.obtenerCodigoMedioPago(paymentItem.getDescription());
                    if(contador == 1){
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

            InvoiceLine invoiceLine;
            InvoiceLineTax invoiceLineTax;
            Integer numberLine = 1;

            double percentajeReservation = 0.00;
            double valorImpuesto;
            double otrosCargos = 0.00;
            List<InvoiceOtherCharges> otherChargesList = new ArrayList<>();

            for (CBInvoiceItem transaction : transactions) {
                //Se procesan las transacciones que no tienen parentId (cargos principales)
                if (Double.parseDouble(transaction.getNetAmount()) > 0 && !transaction.getType().equals("payment")) {

                    if (transaction.getTaxes().size() > 0) {

                        invoiceLine = new InvoiceLine();

                        invoiceLine.setLineNumber(numberLine);
                        numberLine = numberLine + 1;

                        invoiceLine.setProductCode(cbProperty.getProductCodeDefault());
                        if (transaction.getType().equals("rate")) {
                            invoiceLine.setUnidMeasure("Al");
                        } else {
                            invoiceLine.setUnidMeasure("Unid"); //product
                        }
                        invoiceLine.setDescription(transaction.getDescription());
                        invoiceLine.setQuantity(1);

                        invoiceLine.setUnitPrice(Double.parseDouble(transaction.getTotalAmount()));
                        invoiceLine.setDiscountAmount(0);

                        //invoice.getLines().add(invoiceLine);

                        for (CBInvoiceTax tax : transaction.getTaxes()) {
                            if (tax.getAmount() != null && Double.parseDouble(tax.getAmount()) > 0) {
                                valorImpuesto = Double.parseDouble(tax.getAmount());

                                try {
                                    Optional<CBTaxData> taxOptional = cbTaxes.getData().stream()
                                            .filter(t -> t.getTaxID().equals(tax.getTaxID()))
                                            .findFirst();

                                    if (taxOptional.isPresent()) {
                                        if (taxOptional.get().getAmount() != null) {
                                            percentajeReservation = Double.parseDouble(taxOptional.get().getAmount());
                                        }
                                    } else {
                                        percentajeReservation = Math.round((valorImpuesto / Double.parseDouble(transaction.getTotalAmount())) * 100);
                                    }

                                }catch (Exception ex){
                                    logger.error("Error al obtener la informacion del impuesto", ex);
                                    percentajeReservation = Math.round((valorImpuesto / Double.parseDouble(transaction.getTotalAmount())) * 100);
                                }

                                invoiceLineTax = new InvoiceLineTax();
                                invoiceLineTax.setRate(percentajeReservation);
                                invoiceLineTax.setRateTaxCode("08");
                                invoiceLineTax.setTaxCode("01");
                                invoiceLineTax.setAmount(valorImpuesto);
                                invoiceLine.getLineTaxes().add(invoiceLineTax);

                            }
                        }

                        invoice.getLines().add(invoiceLine);
                    }else{
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

            if(otherChargesList.size() > 0){
                invoice.setInvoiceOtherCharges(otherChargesList);
            }

            //Se envia la factura al transmisor específico del país:
            try {
                String jsonInvoice = facturaTransmisor.transmitir(
                        invoice,
                        env.getProperty("atvadapter.rabbitmq.queue"),
                        env.getProperty("atvadapter.rabbitmq.server"),
                        Integer.parseInt(env.getProperty("atvadapter.rabbitmq.port", "5672")),
                        env.getProperty("atvadapter.rabbitmq.userName"),
                        env.getProperty("atvadapter.rabbitmq.password"),
                        env.getProperty("atvadapter.rabbitmq.virtualHost")
                );
                cbInvoice.setJsonDataAtvAdapter(jsonInvoice);
                cbInvoice.setState(CbStateInvoice.PROGRESS);
                cbInvoiceRepository.save(cbInvoice);
            } catch (Exception txEx) {
                //logger.error(String.format("Se presento un error al transmitir la reservación %s de la propiedad %s", reservationIdWebhook, propertyId), txEx);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            //logger.error(String.format("Excepción al procesar el documento fiscal %s en la propiedad %s", invoiceRequest.getId(), propertyId), ex);
        }
    }

    private Optional<CbInvoice> invoiceAlreadyExists(String cloudbedsInvoiceId, String reservationId, Long propertyId, boolean creditNoteRequest) {
        CbInvoiceId invoiceId = new CbInvoiceId();
        invoiceId.setInvoiceId(cloudbedsInvoiceId);
        invoiceId.setReservationId(reservationId);
        invoiceId.setPropertyId(propertyId);
        invoiceId.setType(creditNoteRequest ? CbTypeInvoice.CREDIT_NOTE : CbTypeInvoice.INVOICE);

        return cbInvoiceRepository.findById(invoiceId);
    }


    private CbProperties getCbProperties(Long propertyId) {
        CbProperties cbProperty = propertiesRepository.findById(propertyId)
                .orElseThrow(() -> {
                    String message = String.format(
                            "La propiedad con Id %s no está registrada",
                            propertyId
                    );
                    logger.info(message);
                    return new PropertyNotFoundException(message);
                });

        if (!Boolean.TRUE.equals(cbProperty.getEnable())) {
            logger.info(
                    "La propiedad {} ({}) no está activa",
                    cbProperty.getPropertyId(),
                    cbProperty.getCompanyName()
            );
            throw new PropertyNotFoundException(String.format(
                    "La propiedad %s (%s) no está activa",
                    cbProperty.getPropertyId(),
                    cbProperty.getCompanyName()
            ));
        }

        logger.info(
                "Se obtiene información de la propiedad {} con nombre {}",
                propertyId,
                cbProperty.getCompanyName()
        );

        return cbProperty;
    }

    private boolean isCreditNote(String requestStatus, String cloudbedsInvoiceId, String reservationId) throws Exception {
        // El nuevo flujo fiscal solo debe procesar los eventos pendientes de integración y cancelación.
        final String status = Optional.ofNullable(requestStatus).orElse("");
        if ("CANCEL_REQUESTED".equalsIgnoreCase(status)) {
            logger.info(
                    "La factura {} de la reservación {} está en estado cancel_requested",
                    cloudbedsInvoiceId,
                    reservationId
            );
            return true;
        }

        if ("PENDING_INTEGRATION".equalsIgnoreCase(status)) {
            logger.info(
                    "La factura {} de la reservación {} recibida en estado pending_integration",
                    cloudbedsInvoiceId,
                    reservationId
            );
            return false;
        }

        logger.error(
                "La factura {} de la reservación {} tiene un estado fiscal no procesable para el flujo nuevo: {}",
                cloudbedsInvoiceId,
                reservationId,
                status
        );

        throw new Exception(String.format(
                "La factura %s de la reservación %s tiene un estado fiscal no procesable para el flujo nuevo: %s",
                cloudbedsInvoiceId,
                reservationId,
                status
        ));
    }

    private Optional<Long> resolvePropertyId(InvoiceRequest invoiceRequest) {
        return Optional.ofNullable(invoiceRequest.getPropertyIdText())
                .filter(text -> !text.isEmpty())
                .map(text -> {
                    try {
                        return Long.parseLong(text);
                    } catch (NumberFormatException ex) {
                        logger.error("Error al convertir property id del webhook: {}", text, ex);
                        return null;
                    }
                });
    }

    private Optional<FiscalDocumentContext> getFiscalDocumentContext(String fiscalDocumentId, Long propertyId, String token) throws Exception {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.cloudbeds.com/fiscal-document/v1/fiscal-documents")
                .queryParam("limit", 1)
                .queryParam("filters[ids]", fiscalDocumentId)
                .toUriString();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Mozilla/5.0");
        headers.add("Authorization", String.format("Bearer %s", token));
        headers.add("X-Property-ID", String.valueOf(propertyId));
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new Exception("No fue posible consultar el documento fiscal");
        }

        List<Map<String, Object>> documents = (List<Map<String, Object>>) response.getBody().get("fiscalDocuments");
        if (documents == null || documents.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> document = documents.get(0);

        FiscalDocumentContext context = new FiscalDocumentContext();
        context.setInvoiceReference(valueToString(document.get("externalId")));
        if(context.getInvoiceReference() == null || context.getInvoiceReference().isEmpty()) {
            context.setInvoiceReference(valueToString(document.get("number")));
        }
        if(context.getInvoiceReference() == null || context.getInvoiceReference().isEmpty()) {
            context.setInvoiceReference(fiscalDocumentId);
        }
        context.setReservationId(valueToString(document.get("sourceId")));
        context.setStatus(valueToString(document.get("status")));
        context.setKind(valueToString(document.get("kind")));
        context.setInvoiceDetail(mapFiscalDocumentToLegacyInvoiceResponse(document, fiscalDocumentId));
        return Optional.of(context);
    }

    private CBInvoiceResponse mapFiscalDocumentToLegacyInvoiceResponse(Map<String, Object> document, String fiscalDocumentId) {
        CBInvoiceResponse response = new CBInvoiceResponse();
        response.setSuccess(true);

        CBInvoiceData data = new CBInvoiceData();
        data.setInvoiceID(firstString(document, "externalId", "number", "id"));
        if(data.getInvoiceID() == null || data.getInvoiceID().isEmpty()) {
            data.setInvoiceID(fiscalDocumentId);
        }
        data.setReservationID(firstString(document, "sourceId", "reservationID", "reservationId"));
        data.setStatus(firstString(document, "status"));
        data.setNumber(parseLong(firstString(document, "number")));
        data.setItems(mapFiscalDocumentItems(document));

        response.setData(data);
        response.setStatusCode(200);
        return response;
    }

    private List<CBInvoiceItem> mapFiscalDocumentItems(Map<String, Object> document) {
        List<Map<String, Object>> rawItems = extractMapList(document, "items");
        if(rawItems.isEmpty()) {
            rawItems = extractMapList(document, "lines");
        }
        if(rawItems.isEmpty()) {
            rawItems = extractMapList(document, "documentItems");
        }

        List<CBInvoiceItem> mappedItems = new ArrayList<>();
        for (Map<String, Object> rawItem : rawItems) {
            CBInvoiceItem item = new CBInvoiceItem();
            item.setDescription(firstString(rawItem, "description", "name", "title"));
            String type = firstString(rawItem, "type", "itemType", "category");
            item.setType((type == null || type.isEmpty()) ? "charge" : type.toLowerCase());
            item.setCurrency(firstString(rawItem, "currency", "currencyCode"));

            Double quantity = parseDouble(firstString(rawItem, "quantity"));
            item.setQuantity(quantity != null ? quantity : 1d);

            Double totalAmount = parseDouble(firstString(rawItem, "totalAmount", "amount", "total", "grossAmount"));
            Double netAmount = parseDouble(firstString(rawItem, "netAmount", "subtotal", "net"));
            if(totalAmount == null && netAmount != null) {
                totalAmount = netAmount;
            }
            if(netAmount == null && totalAmount != null) {
                netAmount = totalAmount;
            }

            item.setTotalAmount(formatAmount(totalAmount));
            item.setNetAmount(formatAmount(netAmount));
            item.setTaxes(mapFiscalDocumentTaxes(rawItem));
            mappedItems.add(item);
        }
        return mappedItems;
    }

    private List<CBInvoiceTax> mapFiscalDocumentTaxes(Map<String, Object> rawItem) {
        List<Map<String, Object>> rawTaxes = extractMapList(rawItem, "taxes");
        if(rawTaxes.isEmpty()) {
            rawTaxes = extractMapList(rawItem, "taxLines");
        }

        List<CBInvoiceTax> mappedTaxes = new ArrayList<>();
        for (Map<String, Object> rawTax : rawTaxes) {
            CBInvoiceTax tax = new CBInvoiceTax();
            tax.setTaxID(firstString(rawTax, "taxID", "taxId", "id", "code"));
            tax.setCode(firstString(rawTax, "code", "taxCode"));
            tax.setName(firstString(rawTax, "name", "description", "label"));
            tax.setAmount(formatAmount(parseDouble(firstString(rawTax, "amount", "taxAmount", "total"))));
            mappedTaxes.add(tax);
        }
        return mappedTaxes;
    }

    private List<Map<String, Object>> extractMapList(Map<String, Object> source, String key) {
        Object raw = source.get(key);
        if(!(raw instanceof List)) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> mapped = new ArrayList<>();
        for (Object item : (List<?>) raw) {
            if(item instanceof Map) {
                mapped.add((Map<String, Object>) item);
            }
        }
        return mapped;
    }

    private String firstString(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            String value = valueToString(source.get(key));
            if(value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private Double parseDouble(String value) {
        if(value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if(value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private String formatAmount(Double value) {
        if(value == null) {
            return "0";
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private String valueToString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    // Estado: E: exitoso, R: rechazado
    public void notifyInvoiceCloudbeds(String clave, String estado, String descripcion) {
        try{

            Optional<CbInvoice> invoiceFind = cbInvoiceRepository.findByClaveHacienda(clave);

            if(!invoiceFind.isPresent()){
                logger.warn(String.format("No se encontró la factura con clave hacienda %s para notificar a Cloudbeds", clave));
                return;
            }

            CbInvoice cbInvoice = invoiceFind.get();
            CbProperties cbProperties = cbInvoice.getProperties();
            String reservationId = cbInvoice.getInvoiceId().getReservationId();
            String invoiceId = cbInvoice.getInvoiceId().getInvoiceId();
            boolean isCreditNote = cbInvoice.getInvoiceId().getType().equals(CbTypeInvoice.CREDIT_NOTE);
            String fiscalDocumentId = cbInvoice.getOthersMessage();

            //String base64Document = ebiService.obtenerDocumento(cbProperties, numeroDocumentoFiscal, "000001", (isCreditNote ? "04" : "01"), emmisionType);
            String base64Document = "";

            if(estado.equals("E")){
                logger.info(String.format("La factura %s relacionada a la reservación %s fue procesada exitosamente en Hacienda. Se procede a notificar a Cloudbeds y agregar nota en la reservación", invoiceId, reservationId));
                if(isCreditNote){
                    cbInvoice.setState(CbStateInvoice.VOIDED);
                }else{
                    invoiceFind.get().setState(CbStateInvoice.SENT);
                }
                cbInvoiceRepository.save(invoiceFind.get());

                if(fiscalDocumentId == null || fiscalDocumentId.isEmpty()) {
                    logger.error(String.format("No existe fiscalDocumentId asociado a la clave %s; no se puede notificar a Cloudbeds en el flujo nuevo", clave));
                } else {
                    updateFiscalDocumentStatus(
                            cbProperties.getCbAccount().getApiKey(),
                            cbProperties.getPropertyId(),
                            fiscalDocumentId,
                            isCreditNote ? "CANCELED" : "COMPLETED_INTEGRATION",
                            null,
                            base64Document
                    );
                }

                reservationNote(
                        cbProperties,
                        reservationId,
                        String.format("%s relacionada al consecutivo %s enviada con éxito. Clave: %s", isCreditNote ? "Nota de crédito" : "Factura", invoiceId, clave)
                );
            } else if (estado.equals("R")) {
                logger.info(String.format("La factura %s relacionada a la reservación %s fue rechazada en Hacienda por el siguiente motivo: %s. Se procede a notificar a Cloudbeds y agregar nota en la reservación", invoiceId, reservationId, descripcion));
                cbInvoice.setState(CbStateInvoice.FAILED);
                invoiceFind.get().setRespuestaHacienda(descripcion);
                cbInvoiceRepository.save(invoiceFind.get());

                if(fiscalDocumentId == null || fiscalDocumentId.isEmpty()) {
                    logger.error(String.format("No existe fiscalDocumentId asociado a la clave %s; no se puede notificar rechazo a Cloudbeds en el flujo nuevo", clave));
                } else {
                    updateFiscalDocumentStatus(
                            cbProperties.getCbAccount().getApiKey(),
                            cbProperties.getPropertyId(),
                            fiscalDocumentId,
                            isCreditNote ? "OPEN" : "FAILED",
                            descripcion,
                            base64Document
                    );
                }

                reservationNote(
                        cbProperties,
                        reservationId,
                        String.format("%s relacionada al consecutivo %s se rechazo (%s). Clave: %s", isCreditNote ? "Nota de crédito" : "Factura", invoiceId, descripcion, clave)
                );
            } else {
                logger.warn(String.format("La factura %s relacionada a la reservación %s tiene un estado desconocido en Hacienda: %s.", invoiceId, reservationId, estado));
            }

        }catch (Exception ex){
            ex.printStackTrace();
            logger.error(ex);
        }
    }

    private void updateFiscalDocumentStatus(String apiKey, Long propertyId, String fiscalDocumentId, String status, String failReason, String base64Document) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Authorization", String.format("Bearer %s", apiKey));
            headers.add("X-Property-ID", String.valueOf(propertyId));

            Map<String, Object> body = new HashMap<>();
            body.put("status", status);
            if(failReason != null && !failReason.isEmpty()) {
                body.put("failReason", failReason);
            }

            if("COMPLETED_INTEGRATION".equals(status) && base64Document != null && !base64Document.isEmpty()) {
                Map<String, Object> governmentIntegration = new HashMap<>();
                governmentIntegration.put("pdfFileBase64", base64Document);
                body.put("governmentIntegration", governmentIntegration);
            }

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            String url = String.format("https://api.cloudbeds.com/fiscal-document/v1/fiscal-documents/%s", fiscalDocumentId);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);

            if(response.getStatusCode().is2xxSuccessful()) {
                logger.info(String.format("Documento fiscal %s actualizado correctamente a estado %s", fiscalDocumentId, status));
            } else {
                logger.error(String.format("Error actualizando documento fiscal %s a estado %s: %s", fiscalDocumentId, status, response.getBody()));
            }
        } catch (Exception ex) {
            logger.error(String.format("Excepcion actualizando documento fiscal %s a estado %s", fiscalDocumentId, status), ex);
        }
    }

    @Data
    private static class FiscalDocumentContext {
        private String invoiceReference;
        private String reservationId;
        private String status;
        private String kind;
        private CBInvoiceResponse invoiceDetail;
    }
}
