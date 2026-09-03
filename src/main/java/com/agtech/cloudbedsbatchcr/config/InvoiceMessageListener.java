package com.agtech.cloudbedsbatchcr.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.agtech.cloudbedsbatchcr.pojo.webhook.ReservationNote;
import com.agtech.cloudbedsbatchcr.services.CBService;

@Component
public class InvoiceMessageListener {

    static final Logger logger = LoggerFactory.getLogger(InvoiceMessageListener.class);

    @Autowired
    private CBService cbService;

    @RabbitListener(queues = RabbitConfig.CB_NOTIFICATION_QUEUE)
    public void processInvoice(ReservationNote reservationNote) {
        logger.info(String.format("Nota recibida en queue de facturas: %s, estado: %s, descripcion: %s", reservationNote.getClave(), reservationNote.getEstado(), reservationNote.getDescripcion()));
        cbService.notifyInvoiceCloudbeds(reservationNote.getClave(), reservationNote.getEstado(), reservationNote.getDescripcion());
    }
}
