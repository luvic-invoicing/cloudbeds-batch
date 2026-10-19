package com.agtech.cloudbedsbatchcr.services;

import com.agtech.cloudbedsbatchcr.pojo.webhook.InvoiceRequest;
import com.agtech.cloudbedsbatchcr.pojo.cloudbeds.CBInvoiceItem;
import com.agtech.cloudbedsbatchcr.pojo.cloudbeds.CBInvoiceTax;
import com.agtech.cloudbedsbatchcr.entities.CbAccount;
import com.agtech.cloudbedsbatchcr.repositories.ICbAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.mockito.Mockito.*;

class CBServiceTest {

    private final CBService service = new CBService();

    @Test
    void requestedEventIsIgnoredUntilCloudbedsSetsPendingIntegration() {
        assertTrue(shouldIgnore("REQUESTED"));
    }

    @Test
    void pendingIntegrationEventIsProcessed() {
        assertFalse(shouldIgnore("PENDING_INTEGRATION"));
    }

    @Test
    void cancelRequestedEventIsProcessedAsCreditNote() {
        assertFalse(shouldIgnore("CANCEL_REQUESTED"));
    }

    @Test
    void completedIntegrationEventIsIgnoredToAvoidReprocessing() {
        assertTrue(shouldIgnore("COMPLETED_INTEGRATION"));
    }

    @Test
    void fetchesAndPersistsExchangeWhenLastDateIsNull() {
        assertExchangeRefresh(null, null, 500d);
    }

    @Test
    void refreshesAnOlderExchangeUsingTheSaleIndicator() {
        Date yesterday = Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        assertExchangeRefresh(yesterday, "V", 510d);
    }

    @Test
    void reusesTodaysExchangeWithoutHttpOrDatabaseWrites() {
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "exchangeRestTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ICbAccountRepository repository = mock(ICbAccountRepository.class);
        ReflectionTestUtils.setField(service, "cbAccountRepository", repository);
        CbAccount account = new CbAccount();
        account.setLastExchangeDate(Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        account.setExchange(495d);

        Double exchange = ReflectionTestUtils.invokeMethod(service, "getExchange", account);

        assertEquals(495d, exchange.doubleValue());
        verifyNoInteractions(repository);
        server.verify();
    }

    private void assertExchangeRefresh(Date lastDate, String indicator, double expectedExchange) {
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "exchangeRestTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ICbAccountRepository repository = mock(ICbAccountRepository.class);
        ReflectionTestUtils.setField(service, "cbAccountRepository", repository);
        CbAccount account = new CbAccount();
        account.setLastExchangeDate(lastDate);
        account.setTcIndicator(indicator);
        account.setExchange(495d);
        server.expect(requestTo("https://api.hacienda.go.cr/indicadores/tc/dolar"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"compra\":{\"valor\":500},\"venta\":{\"valor\":510}}",
                        MediaType.APPLICATION_JSON));

        Double exchange = ReflectionTestUtils.invokeMethod(service, "getExchange", account);

        assertEquals(expectedExchange, exchange.doubleValue());
        assertEquals(expectedExchange, account.getExchange().doubleValue());
        assertEquals(LocalDate.now(), account.getLastExchangeDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate());
        verify(repository).save(account);
        server.verify();
    }

    @Test
    void resolvesTypeAndTaxOriginIdWithSeparateIdsAndTheSameEarliestDate() {
        MockRestServiceServer server = accountingServer();
        expectAccounting(server, "externalRelationKind", "ROOM", "item-1", "item-2");
        expectAccounting(server, "originId", "819091", "tax-1");
        Map<String, Object> tax = values("id", "tax-1", "taxId", "not-the-lookup-id",
                "amount", "13", "description", "IVA", "transactionDate", "2026-10-17T00:00:00Z");
        Map<String, Object> first = values("id", "item-1", "transactionDate", "2026-10-19T00:00:00Z",
                "amount", "100", "taxes", Arrays.asList(tax, tax));
        Map<String, Object> second = values("id", "item-2", "transactionDate", "2026-10-18T00:00:00Z");

        List<CBInvoiceItem> items = ReflectionTestUtils.invokeMethod(service, "mapFiscalDocumentItems",
                Arrays.asList(first, second), 123L, "test-token");

        assertEquals("rate", items.get(0).getType());
        assertEquals(2, items.get(0).getTaxes().size());
        assertEquals("819091", items.get(0).getTaxes().get(0).getTaxID());
        assertEquals("819091", items.get(0).getTaxes().get(1).getTaxID());
        assertEquals("IVA", items.get(0).getTaxes().get(0).getName());
        assertEquals(13d, Double.parseDouble(items.get(0).getTaxes().get(0).getAmount()));
        assertTrue(items.get(1).getTaxes().isEmpty());
        server.verify();
    }

    @Test
    void supportsTaxLinesAndNumericOriginIds() {
        MockRestServiceServer server = accountingServer();
        server.expect(requestTo("https://api.cloudbeds.com/accounting/v1.0/transactions"))
                .andExpect(jsonPath("$.filters.and[1].value").value(contains("tax-1")))
                .andRespond(withSuccess("{\"transactions\":[{\"id\":\"tax-1\",\"originId\":819091}]}",
                        MediaType.APPLICATION_JSON));
        List<CBInvoiceTax> taxes = mapTaxes(values("taxLines",
                Collections.singletonList(values("id", "tax-1"))));
        assertEquals("819091", taxes.get(0).getTaxID());
        server.verify();
    }

    @Test
    void rejectsMissingOrEmptyOriginIdsInsteadOfUsingTheTaxTransactionId() {
        for (String response : Arrays.asList("{\"transactions\":[]}",
                "{\"transactions\":[{\"id\":\"tax-1\"}]}",
                "{\"transactions\":[{\"id\":\"tax-1\",\"originId\":\"\"}]}")) {
            MockRestServiceServer server = accountingServer();
            server.expect(requestTo("https://api.cloudbeds.com/accounting/v1.0/transactions"))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
            assertThrows(IllegalStateException.class, () -> mapTaxes(values("taxes",
                    Collections.singletonList(values("id", "tax-1")))));
            server.verify();
        }
    }

    @Test
    void skipsTaxRequestsForEmptyTaxesAndRejectsMissingTaxIds() {
        MockRestServiceServer server = accountingServer();
        assertTrue(mapTaxes(Collections.emptyMap()).isEmpty());
        assertThrows(IllegalStateException.class, () -> mapTaxes(values("taxes",
                Collections.singletonList(values("taxId", "not-the-lookup-id")))));
        server.verify();
    }

    private MockRestServiceServer accountingServer() {
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "accountingRestTemplate");
        return MockRestServiceServer.bindTo(restTemplate).build();
    }

    private void expectAccounting(MockRestServiceServer server, String field, String value, String... ids) {
        server.expect(requestTo("https://api.cloudbeds.com/accounting/v1.0/transactions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-token"))
                .andExpect(header("X-PROPERTY-ID", "123"))
                .andExpect(jsonPath("$.filters.and[1].value").value(contains(ids)))
                .andExpect(jsonPath("$.filters.and[0].value").value("2026-10-18T00:00:00Z"))
                .andExpect(jsonPath("$.limit").value(ids.length))
                .andRespond(withSuccess("{\"transactions\":[{\"id\":\"" + ids[0]
                        + "\",\"" + field + "\":\"" + value + "\"}]}", MediaType.APPLICATION_JSON));
    }

    private List<CBInvoiceTax> mapTaxes(Map<String, Object> transaction) {
        return ReflectionTestUtils.invokeMethod(service, "mapFiscalDocumentTaxes",
                transaction, "2026-10-18T00:00:00Z", 123L, "test-token");
    }

    private Map<String, Object> values(Object... entries) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            result.put((String) entries[i], entries[i + 1]);
        }
        return result;
    }

    private boolean shouldIgnore(String status) {
        InvoiceRequest request = new InvoiceRequest();
        request.setId("fiscal-document-1");
        request.setStatus(status);
        return (Boolean) ReflectionTestUtils.invokeMethod(service, "ignoreEvents", request);
    }
}
