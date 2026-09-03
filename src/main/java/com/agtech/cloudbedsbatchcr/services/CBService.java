package com.agtech.cloudbedsbatchcr.services;

import com.agtech.cloudbedsbatchcr.entities.*;
import com.agtech.cloudbedsbatchcr.pojo.TipoCambio;
import com.agtech.cloudbedsbatchcr.pojo.atvadapter.*;
import com.agtech.cloudbedsbatchcr.pojo.cloudbeds.*;
import com.agtech.cloudbedsbatchcr.pojo.webhook.*;
import com.agtech.cloudbedsbatchcr.repositories.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
public class CBService {

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

                //Se registran los webhooks:
                //1. Creacion de facturas:
                createWebHook("http://144.202.35.15:8050/api/wh/v1/invoice",
                        "reservation",
                        "invoice_requested",
                        property.getApiKey(),
                        property.getPropertyId());

                //2. Nota de crédito de facturas:
                createWebHook("http://144.202.35.15:8050/api/wh/v1/void-invoice",
                        "reservation",
                        "invoice_void_requested",
                        property.getApiKey(),
                        property.getPropertyId());

                //3. Cambio de estado de la integracion:
                createWebHook("http://144.202.35.15:8050/api/wh/v1/changeAppState",
                        "integration",
                        "appstate_changed",
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
    public void invoice(InvoiceRequest invoiceRequest, boolean isCreditNote){
        try{
            boolean enabledId = false;
            String claveReferencia = "";

            ObjectMapper mapper = new ObjectMapper();
            String jsonRecibido = mapper.writeValueAsString(invoiceRequest);
            logger.info(String.format("JSON recibido %s", jsonRecibido));

            Optional<CbProperties> cbProperty = propertiesRepository.findById(invoiceRequest.getPropertyID());
            if (cbProperty.isPresent()) {
                if(!cbProperty.get().getEnable()){
                    logger.info(String.format("La propiedad %s (%s) no esta activa", cbProperty.get().getPropertyId(), cbProperty.get().getCompanyName()));
                    return;
                }
                logger.info(String.format("Se obtiene informacion de la propiedad %s con nombre %s", invoiceRequest.getPropertyID(), cbProperty.get().getCompanyName()));
            } else {
                logger.info(String.format("La propiedad con Id %s no esa registrada", invoiceRequest.getPropertyID()));
                throw new Exception(String.format("La propiedad con Id %s no esa registrada", invoiceRequest.getPropertyID()));
            }

            //Se buscan los datos de la factura
            CBInvoiceResponse cbInvoiceResponse = getInvoiceDetail(invoiceRequest.getInvoiceID(), invoiceRequest.getPropertyID().toString(), cbProperty.get().getCbAccount().getApiKey());

            logger.info(mapper.writeValueAsString(cbInvoiceResponse));

            //Se valida que la factura este en los estados open o voided
            if(!isCreditNote && cbInvoiceResponse.getData().getStatus().equals("requested")) {
                logger.info(String.format("La factura %s de la reservacion %s recibida", invoiceRequest.getInvoiceID(), cbInvoiceResponse.getData().getReservationID()));
            } else if(isCreditNote && cbInvoiceResponse.getData().getStatus().equals("void_requested")) {
                logger.info(String.format("La factura %s de la reservacion %s esta en estado anulado", invoiceRequest.getInvoiceID(), cbInvoiceResponse.getData().getReservationID()));
            }else{
                logger.info(String.format("La factura %s de la reservacion %s tiene un estado diferente a open y voided: %s", invoiceRequest.getInvoiceID(), cbInvoiceResponse.getData().getReservationID(), cbInvoiceResponse.getData().getStatus()));
                return;
            }

            CbInvoiceId invoiceId = new CbInvoiceId();
            invoiceId.setInvoiceId(invoiceRequest.getInvoiceID());
            invoiceId.setReservationId(cbInvoiceResponse.getData().getReservationID());
            invoiceId.setPropertyId(invoiceRequest.getPropertyID());
            invoiceId.setType(isCreditNote ? CbTypeInvoice.CREDIT_NOTE : CbTypeInvoice.INVOICE);

            Optional<CbInvoice> invoiceFind = cbInvoiceRepository.findById(invoiceId);

            if(invoiceFind.isPresent()){
                logger.warn(String.format("Ya existe la %s %s de la reservacion %s registrada en el sistema", (isCreditNote ? "Nota de crédito" : "Factura"), invoiceRequest.getInvoiceID(), cbInvoiceResponse.getData().getReservationID()));
                return;
            }

            Optional<CbInvoice> originalInvoice = null;

            if(isCreditNote){
                //Se busca la factura original para validar que exista y no este anulada
                CbInvoiceId originalInvoiceId = new CbInvoiceId();
                originalInvoiceId.setInvoiceId(invoiceRequest.getInvoiceID());
                originalInvoiceId.setReservationId(cbInvoiceResponse.getData().getReservationID());
                originalInvoiceId.setPropertyId(invoiceRequest.getPropertyID());
                originalInvoiceId.setType(CbTypeInvoice.INVOICE);

                originalInvoice = cbInvoiceRepository.findById(originalInvoiceId);

                if(!originalInvoice.isPresent()){
                    logger.warn(String.format("La factura %s de la reservacion %s no existe para ser anulada en el sistema", invoiceRequest.getInvoiceID(), cbInvoiceResponse.getData().getReservationID()));
                    return;
                } else if(originalInvoice.isPresent() && originalInvoice.get().getState().equals(CbStateInvoice.VOIDED)){
                    logger.warn(String.format("La factura %s de la reservacion %s ya se encuentra anulada", invoiceRequest.getInvoiceID(), cbInvoiceResponse.getData().getReservationID()));
                    return;
                }else{
                    claveReferencia = originalInvoice.get().getClaveHacienda();
                }
            }

            //Se crea la entidad reservation para guardar los datos
            CbInvoice cbInvoice = new CbInvoice();
            cbInvoice.setDate(new Date());
            cbInvoice.setInvoiceId(invoiceId);
            cbInvoice.setProperties(cbProperty.get());
            cbInvoiceRepository.save(cbInvoice);

            //Se obtienen los impuestos parametrizados:
            CBTaxes cbTaxes = taxService.getTaxes(cbProperty.get(), cbProperty.get().getCbAccount().getApiKey());

            logger.info(mapper.writeValueAsString(cbTaxes));

            //Se obtiene la información de la reservación:
            CBReservationInfoResponse cbReservation = getReservationInformation(cbProperty.get(), invoiceId.getReservationId());

            cbInvoice.setJsonDataReservation(mapper.writeValueAsString(cbReservation));
            cbInvoice.setJsonDataInvoice(mapper.writeValueAsString(cbInvoiceResponse));
            cbInvoice.setConsecutiveInvoice(cbInvoiceResponse.getData().getNumber());
            cbInvoice.setJsonRequestCb(jsonRecibido);

            Invoice invoice = new Invoice();
            invoice.setCustomerId(cbProperty.get().getTaxIdentificacion());
            invoice.setCode(cbProperty.get().getCurrency());
            if(cbProperty.get().getCurrency().equals("USD")){
                invoice.setExchange(getExchange(cbProperty.get().getCbAccount()));
            }else{
                invoice.setExchange(1.00);
            }
            invoice.setSystem(ErpSystem.CLOUD_BEDS_HOTEL);

            if(isCreditNote){
                invoice.setDocumentType(DocumentType.NCT);
            }else {
                invoice.setDocumentType(DocumentType.T);
            }

            //Se obtiene la infomación del cliente.
            CBGuestInfo cbGuest = cbReservation.getData().getGuestList()
                    .values()
                    .stream()
                    .findFirst()
                    .orElse(null);
            logger.info(String.format("El tipo de documento registrado para el huesped %s en la reservación %s es %s", cbGuest.getGuestFirstName(), cbInvoiceResponse.getData().getReservationID(), cbGuest.getGuestDocumentType()));

            String taxCompanyId = cbGuest.getCompanyTaxID();
            String documentType = cbGuest.getGuestDocumentType().toLowerCase();

            //Primero se valida que sea una empresa:
            if (taxCompanyId != null && !taxCompanyId.isEmpty()) {
                try {

                    invoice.setClientIdType("02");
                    invoice.setClientId(taxCompanyId);
                    invoice.setClientName(cbGuest.getCompanyName());
                    invoice.setClientEmail(cbGuest.getGuestEmail());
                    if(isCreditNote) {
                        invoice.setDocumentType(DocumentType.NCF);
                    }else{
                        invoice.setDocumentType(DocumentType.F);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    logger.error(ex);
                }
            }else if (documentType != null && !documentType.isEmpty()){
                try {
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
                        if(isCreditNote) {
                            invoice.setDocumentType(DocumentType.NCF);
                        }else{
                            invoice.setDocumentType(DocumentType.F);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    logger.error(String.format("Se presentó un problema al procesar el cliente %s de la factura %s en la empresa %s", cbGuest.getGuestFirstName(), cbInvoiceResponse.getData().getReservationID(), cbProperty.get().getCompanyName()));
                    logger.error(ex);
                }
            }

            KeyGeneratorRequest request = new KeyGeneratorRequest();
            request.setTipoDoc(isCreditNote ? "03" : (invoice.getDocumentType() == DocumentType.F ? "01": "04")); //Tipo de documento.
            if(cbProperty.get().getPosNumber() != null){
                request.setPuntoVenta(Integer.parseInt(cbProperty.get().getPosNumber()));
                request.setMatriz(Integer.parseInt(cbProperty.get().getHeadOfficeNumber()));
                invoice.setPointOfSale(Integer.parseInt(cbProperty.get().getPosNumber()));
            }else{
                request.setPuntoVenta(1);
                request.setMatriz(1);
                invoice.setPointOfSale(1);
            }
            request.setNumIdentification(cbProperty.get().getTaxIdentificacion());
            request.setProdSequence(true);
            KeyGeneratorResponse response = utilService.generateKey(request);
            invoice.setSecuencia(response.getCurrentConsecutiveNumber());
            invoice.setFiscalConsecutive(response.getConsecutiveNumber());
            invoice.setBillKey(response.getVoucherKey());
            invoice.setIdErp(invoiceRequest.getInvoiceID());
            cbInvoice.setClaveHacienda(response.getVoucherKey());
            cbInvoice.setConsecutivoHacienda(response.getConsecutiveNumber());
            cbInvoice.setState(CbStateInvoice.PROGRESS);

            if(isCreditNote) {
                invoice.setReference(claveReferencia);
                invoice.setOtherText(String.format("Nota de crédito a factura %s", cbInvoiceResponse.getData().getNumber()));
            }

            logger.info(String.format("Se crea la clave %s en la reservación %s", response.getVoucherKey(), cbInvoiceResponse.getData().getReservationID()));

            invoice.setCodeSellCondition("01");

            List<CBInvoiceItem> transactions = cbInvoiceResponse.getData().getItems();

            List<CBInvoiceItem> paymentsList = transactions.stream()
                    .filter(t -> t.getType().equals("payment"))
                    .collect(Collectors.toList());

            //Medios de pago.
            String codePaid = "01";
            CodesPaid codesPaid = new CodesPaid();
            if (paymentsList.size() > 0) {
                int contador = 1;
                for (CBInvoiceItem paymentItem : paymentsList) {
                    switch (paymentItem.getDescription().toLowerCase()) {
                        case "credit card":
                            codePaid = "02";   //Tarjeta Crédito
                            break;
                        case "tarjeta de credito":
                            codePaid = "02";   //Tarjeta Crédito
                            break;
                        case "cash":
                            codePaid = "01";   //Efectivo
                            break;
                        case "efectivo":
                            codePaid = "01";   //Efectivo
                            break;
                        case "check":
                            codePaid = "02";   //Tarjeta Crédito
                            break;
                        case "tarjeta de debito":
                            codePaid = "02";   //Tarjeta Crédito
                            break;
                        case "bank transfer":
                            codePaid = "04";   //Transf/Deposito cta. Bancaria
                            break;
                        case "transferencia bancaria":
                            codePaid = "04";   //Transf/Deposito cta. Bancaria
                            break;
                        default:
                            codePaid = "01";   //Otro
                            // Autcompletar descripcion hasta 10 caracteres con * si la descripción es menor a 10 caracteres y no mayor a 100
                            /*
                            String desc = paymentItem.getDescription();
                            if (desc.length() < 10) {
                                desc = String.format("%-" + 10 + "s", desc).replace(' ', '*');
                            } else if (desc.length() > 100) {
                                desc = desc.substring(0, 100);
                            }
                             */
                            break;
                    }
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

                        invoiceLine.setProductCode(cbProperty.get().getProductCodeDefault());
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

            //Se envia la factura de compra al servicio de rabbit MQ:
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUsername(env.getProperty("atvadapter.rabbitmq.userName"));
            factory.setPassword(env.getProperty("atvadapter.rabbitmq.password"));
            factory.setVirtualHost(env.getProperty("atvadapter.rabbitmq.virtualHost"));
            factory.setPort(5672);
            factory.setHost(env.getProperty("atvadapter.rabbitmq.server"));
            try (Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {
                channel.queueDeclare(env.getProperty("atvadapter.rabbitmq.queue"), true, false, false, null);

                ObjectMapper objectMapper = new ObjectMapper();
                String jsonInvoice = objectMapper.writeValueAsString(invoice);
                cbInvoice.setJsonDataAtvAdapter(jsonInvoice);
                cbInvoice.setState(CbStateInvoice.PROGRESS);

                channel.basicPublish("", env.getProperty("atvadapter.rabbitmq.queue"), null, jsonInvoice.getBytes());

                cbInvoiceRepository.save(cbInvoice);

            } catch (IOException e1) {
                e1.printStackTrace();
                logger.error(String.format("Se presento un error 1 en la reservación %s de la propiedad %s", invoiceRequest.getReservationID(), invoiceRequest.getPropertyID()), e1);
            } catch (TimeoutException e2) {
                e2.printStackTrace();
                logger.error(String.format("Se presento un error 2 en la reservación %s de la propiedad %s", invoiceRequest.getReservationID(), invoiceRequest.getPropertyID()), e2);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(String.format("Excepción al procesar la la factura %s en la propiedad %s", invoiceRequest.getInvoiceID(), invoiceRequest.getPropertyID()), ex);
        }
    }

    /**
     * Obtiene el detalle de la factura desde Cloudbeds v1.2
     */
    public CBInvoiceResponse getInvoiceDetail(String invoiceID, String propertyId, String token) throws Exception {
        String url = String.format("https://api.cloudbeds.com/api/v1.2/getInvoice?invoiceID=%s&propertyID=%s", invoiceID, propertyId);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Mozilla/5.0");
        headers.add("Authorization", String.format("Bearer %s", token));
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<CBInvoiceResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, CBInvoiceResponse.class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody();
        } else {
            throw new Exception("Error al obtener el detalle de la factura: " + response.getStatusCode());
        }
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
                postPatchInvoice(cbProperties.getCbAccount().getApiKey(), invoiceId, isCreditNote ? "voided" : "paid", base64Document, cbProperties.getPropertyId().toString());

                //Se agrega la nota en la reservación con el consecutivo de la factura electrónica:
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
                postPatchInvoice(cbProperties.getCbAccount().getApiKey(), invoiceId, "failed", base64Document, cbProperties.getPropertyId().toString());

                //Se agrega la nota en la reservación con el consecutivo de la factura electrónica:
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

    /**
     * Envía el documento (base64) a Cloudbeds usando el endpoint POST https://api.cloudbeds.com/api/v1.2/patchInvoice
     * El body debe ser form-data con los keys: invoiceID (text), status (text='paid'), file (file)
     */
    private void postPatchInvoice(String apiKey, String invoiceID, String status, String base64Document, String propertyID) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.add("x-api-key", apiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("invoiceID", invoiceID);
            body.add("status", status);

            /*
            if((status.equals("paid") || status.equals("voided")) && base64Document != null && !base64Document.isEmpty()) {

                // Decodificar base64 y crear recurso de archivo en memoria
                byte[] fileBytes = Base64.getDecoder().decode(base64Document);

                // Spring's ByteArrayResource para multipart
                org.springframework.core.io.ByteArrayResource fileAsResource = new org.springframework.core.io.ByteArrayResource(fileBytes) {
                    @Override
                    public String getFilename() {
                        return String.format("%s-%s.pdf", (status.equals("paid") ? "invoice" : "credit-note"), invoiceID);
                    }
                };
                body.add("file", fileAsResource);
            }
             */

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String url = String.format("https://api.cloudbeds.com/api/v1.2/patchInvoice?propertyID=%s", propertyID);

            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                // La API responde { "success": true }
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    java.util.Map<String, Object> map = mapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>(){});
                    Object success = map.get("success");
                    if (Boolean.TRUE.equals(success) || (success instanceof String && Boolean.parseBoolean((String) success))) {
                        logger.info(String.format("patchInvoice enviado correctamente para invoice %s", invoiceID));
                    } else {
                        logger.error(String.format("patchInvoice enviado pero 'success' != true para invoice %s - body: %s", invoiceID, response.getBody()));
                    }
                } catch (Exception parseEx) {
                    logger.warn(String.format("No se pudo parsear la respuesta de patchInvoice para invoice %s: %s", invoiceID, parseEx.getMessage()));
                    logger.info(String.format("Respuesta: %s", response.getBody()));
                }
            } else {
                logger.error(String.format("Error al enviar patchInvoice para invoice %s: %s - %s", invoiceID, response.getStatusCode(), response.getBody()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(String.format("Excepcion al enviar patchInvoice para invoice %s", invoiceID), ex);
        }
    }
}
