package com.agtech.cloudbedsbatchcr.services;

import com.agtech.cloudbedsbatchcr.entities.CbProperties;
import com.agtech.cloudbedsbatchcr.pojo.atvadapter.DocumentType;
import com.agtech.cloudbedsbatchcr.pojo.atvadapter.Invoice;
import com.agtech.cloudbedsbatchcr.pojo.atvadapter.KeyGeneratorRequest;
import com.agtech.cloudbedsbatchcr.pojo.atvadapter.KeyGeneratorResponse;
import com.agtech.cloudbedsbatchcr.pojo.cloudbeds.CBGuestInfo;
import com.agtech.cloudbedsbatchcr.pojo.cloudbeds.CBInvoiceData;
import com.agtech.cloudbedsbatchcr.pojo.cloudbeds.CBInvoiceResponse;
import com.agtech.cloudbedsbatchcr.pojo.cloudbeds.CBReservationInfo;
import com.agtech.cloudbedsbatchcr.pojo.cloudbeds.CBReservationInfoResponse;
import com.agtech.cloudbedsbatchcr.pojo.cloudbeds.CBTaxes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoFiscalStrategyTest {

    @Mock
    private UtilService utilService;

    @Test
    void pendingIntegrationBuildsInvoiceWithInvoiceDocumentCode() {
        PendingIntegrationStrategy strategy = new PendingIntegrationStrategy();
        configureKeyGenerator(strategy);

        Invoice invoice = strategy.construirInvoice(property(), reservationWithDniGuest(), invoiceResponse(), "document-123", new CBTaxes(), 1.0);

        ArgumentCaptor<KeyGeneratorRequest> requestCaptor = ArgumentCaptor.forClass(KeyGeneratorRequest.class);
        verify(utilService).generateKey(requestCaptor.capture());
        assertTrue(strategy.supports("PENDING_INTEGRATION"));
        assertFalse(strategy.isCreditNote());
        assertEquals(DocumentType.F, invoice.getDocumentType());
        assertEquals("01", requestCaptor.getValue().getTipoDoc());
        assertEquals("document-123", invoice.getIdErp());
    }

    @Test
    void cancelRequestedBuildsCreditNoteAndReferencesOriginalInvoice() {
        CancelRequestedStrategy strategy = new CancelRequestedStrategy();
        configureKeyGenerator(strategy);

        Invoice invoice = strategy.construirInvoice(property(), reservationWithDniGuest(), invoiceResponse(), "document-123", new CBTaxes(), 1.0);
        strategy.aplicarReferencia(invoice, 135L, "original-hacienda-key");

        ArgumentCaptor<KeyGeneratorRequest> requestCaptor = ArgumentCaptor.forClass(KeyGeneratorRequest.class);
        verify(utilService).generateKey(requestCaptor.capture());
        assertTrue(strategy.supports("CANCEL_REQUESTED"));
        assertTrue(strategy.isCreditNote());
        assertEquals(DocumentType.NCF, invoice.getDocumentType());
        assertEquals("03", requestCaptor.getValue().getTipoDoc());
        assertEquals("original-hacienda-key", invoice.getReference());
        assertEquals("Nota de credito a factura 135", invoice.getOtherText());
    }

    @Test
    void providerResolvesEachWorkflowByItsStatus() {
        DocumentoFiscalStrategyProvider provider = new DocumentoFiscalStrategyProvider(
                java.util.Arrays.asList(new PendingIntegrationStrategy(), new CancelRequestedStrategy()));

        assertTrue(provider.resolve("PENDING_INTEGRATION") instanceof PendingIntegrationStrategy);
        assertTrue(provider.resolve("CANCEL_REQUESTED") instanceof CancelRequestedStrategy);
    }

    private void configureKeyGenerator(DocumentoFiscalStrategy strategy) {
        ReflectionTestUtils.setField(strategy, "utilService", utilService);
        KeyGeneratorResponse response = new KeyGeneratorResponse();
        response.setCurrentConsecutiveNumber(1L);
        response.setConsecutiveNumber("00100001040000000111");
        response.setVoucherKey("50623092600030424054000100001040000000111104002678");
        when(utilService.generateKey(any(KeyGeneratorRequest.class))).thenReturn(response);
    }

    private CbProperties property() {
        CbProperties property = new CbProperties();
        property.setTaxIdentificacion("304240540");
        property.setCurrency("CRC");
        property.setPosNumber("1");
        property.setHeadOfficeNumber("1");
        property.setProductCodeDefault("6311100000000");
        return property;
    }

    private CBReservationInfoResponse reservationWithDniGuest() {
        CBGuestInfo guest = new CBGuestInfo();
        guest.setGuestFirstName("David");
        guest.setGuestLastName("Salas");
        guest.setGuestEmail("david@example.com");
        guest.setGuestDocumentType("dni");
        guest.setGuestDocumentNumber("123456789");

        CBReservationInfo reservation = new CBReservationInfo();
        reservation.setGuestList(new HashMap<>());
        reservation.getGuestList().put("guest-1", guest);

        CBReservationInfoResponse response = new CBReservationInfoResponse();
        response.setData(reservation);
        return response;
    }

    private CBInvoiceResponse invoiceResponse() {
        CBInvoiceData data = new CBInvoiceData();
        data.setItems(Collections.emptyList());
        CBInvoiceResponse response = new CBInvoiceResponse();
        response.setData(data);
        return response;
    }
}
