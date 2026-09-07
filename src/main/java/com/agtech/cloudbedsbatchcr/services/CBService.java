package com.agtech.cloudbedsbatchcr.services;

import com.agtech.cloudbedsbatchcr.entities.*;
import com.agtech.cloudbedsbatchcr.exceptions.DocumentoFiscalNotFoundException;
import com.agtech.cloudbedsbatchcr.exceptions.PropertyNotFoundException;
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
    private TaxService taxService;

    @Autowired
    private DocumentoFiscalStrategyProvider documentoFiscalStrategyProvider;

    @Autowired
    private RabbitInvoicePublisher rabbitInvoicePublisher;

    public PropertyResponse createProperty(Property property) throws Exception {
        try {
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

            if (responseEntity.getBody().getSuccess()) {
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
            } else {
                logger.info(String.format("Se presentó un problema al consultar los datos de la propiedad con identificación %s", property.getTaxIdentification()));
                throw new Exception(String.format("Se presentó un problema al consultar los datos de la propiedad con identificación %s", property.getTaxIdentification()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(String.format("Se presentó un error al crear el cliente con identificación %s", property.getTaxIdentification()), ex);
            throw ex;
        }
    }

    private void createWebHook(String endpointUrl, String object, String action, String apiKey, String propertyID) {
        try {

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
                    String.format("%s?propertyID=%s", "https://api.cloudbeds.com/api/v1.3/postWebhook", propertyID),
                    HttpMethod.POST,
                    requestEntity,
                    CBWebhookResponse.class
            );

            CBWebhookResponse response = responseEntity.getBody();
            if (response.getSuccess()) {
                logger.info(String.format("Webhook registrado correctamente: %s", response.getData().getSubscriptionID()));
            } else {
                logger.error(String.format("Se presentó un problema al crear el webhook con los siguiente parametro: %s -> %s -> %s",
                        endpointUrl,
                        object,
                        action
                ));
            }

        } catch (Exception ex) {
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

    public void changeAppState(AppState appState) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            logger.info(String.format("JSON recibido %s", mapper.writeValueAsString(appState)));

            Optional<CbProperties> property = propertiesRepository.findById(appState.getPropertyID());

            if (!property.isPresent()) {
                logger.info(String.format("La propiedad %s no esta parametrizada en el sistema", appState.getPropertyID()));
                return;
            }

            if (appState.getNewState().equals("enabled")) {
                property.get().setEnable(true);
                logger.info(String.format("Se activa la propiedad %s (%s)", property.get().getPropertyId(), property.get().getCompanyName()));
            } else {
                property.get().setEnable(false);
                logger.info(String.format("Se inactiva la propiedad %s (%s)", property.get().getPropertyId(), property.get().getCompanyName()));
            }
            propertiesRepository.save(property.get());
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(String.format("Excepción al cambiar el estado de la App de la propiedad %s del estado %s al %s", appState.getPropertyID(), appState.getOldState(), appState.getNewState()));
        }
    }

    public void reservationNote(CbProperties cbProperties, String reservationId, String reservationNoteText) {
        try {
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
        } catch (Exception ex) {
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

    private Double getExchange(CbAccount account) {
        try {
            LocalDate fechaActual = LocalDate.now();
            logger.info(String.format("Se compara la fecha actual %s con la ultima fecha guardada %s", fechaActual, account.getLastExchangeDate()));
            if (!account.getLastExchangeDate().equals(fechaActual)) {
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
                if (account.getTcIndicator() != null && account.getTcIndicator().equals("V")) {
                    tipoCambioBCCR = tipoCambio.getVenta().getValor();
                }
                account.setExchange(tipoCambioBCCR);
                account.setLastExchangeDate(new Date());
                cbAccountRepository.save(account);
                return tipoCambioBCCR;
            } else {
                logger.info(String.format("Se usa el ultimo tipo de cambio registrado %s", account.getExchange()));
                return account.getExchange();
            }
        } catch (Exception ex) {
            logger.error(ex);
            return account.getExchange();
        }
    }

    @Transactional
    public void invoice(InvoiceRequest invoiceRequest) {
        try {
            String jsonRecibido = MAPPER.writeValueAsString(invoiceRequest);
            logger.info(String.format("JSON recibido %s", jsonRecibido));

            final Long propertyId = resolvePropertyId(invoiceRequest)
                    .orElseThrow(() -> new PropertyNotFoundException("La propertyId no puede ser encontrada"));

            final String fiscalDocumentId = invoiceRequest.getId();
            final CbProperties cbProperty = getCbProperties(propertyId);

            final FiscalDocumentContext fiscalDocument = getFiscalDocumentContext(fiscalDocumentId, propertyId, cbProperty.getCbAccount().getApiKey())
                    .orElseThrow(() -> new DocumentoFiscalNotFoundException(String.format("No existe documento fiscal para id %s", fiscalDocumentId)));

            final CBInvoiceResponse cbInvoiceResponse = fiscalDocument.getInvoiceDetail();
            final String reservationId = fiscalDocument.getReservationId();
            String cloudbedsInvoiceId = fiscalDocument.getInvoiceReference();
            if (cloudbedsInvoiceId == null || cloudbedsInvoiceId.isEmpty()) {
                cloudbedsInvoiceId = fiscalDocumentId;
            }

            final DocumentoFiscalStrategy strategy = documentoFiscalStrategyProvider.resolve(invoiceRequest.getStatus());
            boolean creditNote = strategy.isCreditNote();

            logger.info(MAPPER.writeValueAsString(cbInvoiceResponse));
            final CbInvoiceId incomingCbInvoiceId = getCbInvoiceId(cloudbedsInvoiceId, reservationId, propertyId, creditNote ? CbTypeInvoice.CREDIT_NOTE : CbTypeInvoice.INVOICE);

            final Optional<CbInvoice> invoiceFound = cbInvoiceRepository.findById(incomingCbInvoiceId);
            if (invoiceFound.isPresent()) {
                logger.warn(String.format("Ya existe la %s %s de la reservacion %s registrada en el sistema", creditNote ? "Nota de crédito" : "Factura", cloudbedsInvoiceId, reservationId));
                return;
            }

            CbInvoice cbInvoice = new CbInvoice();
            cbInvoice.setDate(new Date());
            cbInvoice.setInvoiceId(incomingCbInvoiceId);
            cbInvoice.setProperties(cbProperty);
            cbInvoiceRepository.save(cbInvoice);

            final CBReservationInfoResponse cbReservation = getReservationInformation(cbProperty, reservationId);

            cbInvoice.setJsonDataReservation(MAPPER.writeValueAsString(cbReservation));
            cbInvoice.setJsonDataInvoice(MAPPER.writeValueAsString(cbInvoiceResponse));
            cbInvoice.setConsecutiveInvoice(cbInvoiceResponse.getData().getNumber());
            cbInvoice.setJsonRequestCb(jsonRecibido);
            cbInvoice.setOthersMessage(fiscalDocumentId);

            // Delegar construcción del invoice a la estrategia correspondiente
            Invoice invoice;
            Double exchange = "USD".equals(cbProperty.getCurrency()) ? getExchange(cbProperty.getCbAccount()) : 1.00;
            final CBTaxes cbTaxes = taxService.getTaxes(cbProperty, cbProperty.getCbAccount().getApiKey());
            logger.info(MAPPER.writeValueAsString(cbTaxes));

            if (creditNote) {
                CbInvoiceId originalInvoiceId = getCbInvoiceId(cloudbedsInvoiceId, reservationId, propertyId, CbTypeInvoice.INVOICE);
                final Optional<CbInvoice> originalInvoice = findInvoiceForCreditNote(originalInvoiceId);
                if (!originalInvoice.isPresent()) {
                    logger.warn(String.format("La factura %s de la reservacion %s no existe para ser anulada en el sistema", cloudbedsInvoiceId, reservationId));
                    return;
                }
                if (CbStateInvoice.VOIDED.equals(originalInvoice.get().getState())) {
                    logger.warn(String.format("La factura %s de la reservacion %s ya se encuentra anulada", cloudbedsInvoiceId, reservationId));
                    return;
                }
                final String claveReferencia = originalInvoice.get().getClaveHacienda();
                invoice = strategy.construirInvoice(cbProperty, cbReservation, cbInvoiceResponse, cloudbedsInvoiceId, cbTaxes, exchange);
                // Aplicar referencia de la factura original
                strategy.aplicarReferencia(invoice, cbInvoiceResponse.getData().getNumber(), claveReferencia);
            } else {
                invoice = strategy.construirInvoice(cbProperty, cbReservation, cbInvoiceResponse, cloudbedsInvoiceId, cbTaxes, exchange);
            }

            // Guardar datos de clave y consecutivo desde el invoice construido
            cbInvoice.setClaveHacienda(invoice.getBillKey());
            cbInvoice.setConsecutivoHacienda(invoice.getFiscalConsecutive());
            cbInvoice.setState(CbStateInvoice.PROGRESS);

            final String jsonInvoice = rabbitInvoicePublisher.publish(invoice);
            cbInvoice.setJsonDataAtvAdapter(jsonInvoice);
            cbInvoice.setState(CbStateInvoice.PROGRESS);
            cbInvoiceRepository.save(cbInvoice);
        } catch (Exception ex) {
            logger.error(String.format("Excepción al procesar el documento fiscal %s", invoiceRequest.getId()), ex);
        }
    }

    private Optional<CbInvoice> findInvoiceForCreditNote(CbInvoiceId originalInvoiceId) {
        return cbInvoiceRepository.findById(originalInvoiceId);
    }

    private static CbInvoiceId getCbInvoiceId(String cloudbedsInvoiceId, String reservationId, Long propertyId, CbTypeInvoice creditNoteRequest) {
        CbInvoiceId invoiceId = new CbInvoiceId();
        invoiceId.setInvoiceId(cloudbedsInvoiceId);
        invoiceId.setReservationId(reservationId);
        invoiceId.setPropertyId(propertyId);
        invoiceId.setType(creditNoteRequest);
        return invoiceId;
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
                .queryParam("ids", fiscalDocumentId)
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
        if (context.getInvoiceReference() == null || context.getInvoiceReference().isEmpty()) {
            context.setInvoiceReference(valueToString(document.get("number")));
        }
        if (context.getInvoiceReference() == null || context.getInvoiceReference().isEmpty()) {
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
        if (data.getInvoiceID() == null || data.getInvoiceID().isEmpty()) {
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
        if (rawItems.isEmpty()) {
            rawItems = extractMapList(document, "lines");
        }
        if (rawItems.isEmpty()) {
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
            if (totalAmount == null && netAmount != null) {
                totalAmount = netAmount;
            }
            if (netAmount == null && totalAmount != null) {
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
        if (rawTaxes.isEmpty()) {
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
        if (!(raw instanceof List)) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> mapped = new ArrayList<>();
        for (Object item : (List<?>) raw) {
            if (item instanceof Map) {
                mapped.add((Map<String, Object>) item);
            }
        }
        return mapped;
    }

    private String firstString(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            String value = valueToString(source.get(key));
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private Double parseDouble(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private String formatAmount(Double value) {
        if (value == null) {
            return "0";
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private String valueToString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    // Estado: E: exitoso, R: rechazado
    public void notifyInvoiceCloudbeds(String clave, String estado, String descripcion) {
        try {

            Optional<CbInvoice> invoiceFind = cbInvoiceRepository.findByClaveHacienda(clave);

            if (!invoiceFind.isPresent()) {
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

            if (estado.equals("E")) {
                logger.info(String.format("La factura %s relacionada a la reservación %s fue procesada exitosamente en Hacienda. Se procede a notificar a Cloudbeds y agregar nota en la reservación", invoiceId, reservationId));
                if (isCreditNote) {
                    cbInvoice.setState(CbStateInvoice.VOIDED);
                } else {
                    invoiceFind.get().setState(CbStateInvoice.SENT);
                }
                cbInvoiceRepository.save(invoiceFind.get());

                if (fiscalDocumentId == null || fiscalDocumentId.isEmpty()) {
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

                if (fiscalDocumentId == null || fiscalDocumentId.isEmpty()) {
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

        } catch (Exception ex) {
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
            if (failReason != null && !failReason.isEmpty()) {
                body.put("failReason", failReason);
            }

            if ("COMPLETED_INTEGRATION".equals(status) && base64Document != null && !base64Document.isEmpty()) {
                Map<String, Object> governmentIntegration = new HashMap<>();
                governmentIntegration.put("pdfFileBase64", base64Document);
                body.put("governmentIntegration", governmentIntegration);
            }

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            String url = String.format("https://api.cloudbeds.com/fiscal-document/v1/fiscal-documents/%s", fiscalDocumentId);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
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
