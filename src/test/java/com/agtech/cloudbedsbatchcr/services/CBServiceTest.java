package com.agtech.cloudbedsbatchcr.services;

import com.agtech.cloudbedsbatchcr.pojo.webhook.InvoiceRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private boolean shouldIgnore(String status) {
        InvoiceRequest request = new InvoiceRequest();
        request.setId("fiscal-document-1");
        request.setStatus(status);
        return (Boolean) ReflectionTestUtils.invokeMethod(service, "ignoreEvents", request);
    }
}
