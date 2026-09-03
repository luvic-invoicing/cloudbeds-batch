package com.agtech.cloudbedsbatchcr.controller;

import com.agtech.cloudbedsbatchcr.pojo.webhook.*;
import com.agtech.cloudbedsbatchcr.services.CBService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/wh/v1")
public class whController {

    private static final Logger logger = LogManager.getLogger(whController.class);

    @Autowired
    private CBService cbService;

    @GetMapping("/test")
    public Boolean test()  {
        logger.info("Servicio disponible");
        return true;
    }

    @PostMapping("/inbound")
    public ResponseEntity inbound(@RequestBody StatusChanged statusChanged) {
        try{
            logger.info(String.format("Se recibe la reservacion %s del hotel %s", statusChanged.getReservationID(), statusChanged.getPropertyID()));
            //cbService.inbound(statusChanged);
            return new ResponseEntity<>(HttpStatus.OK);
        }catch (Exception ex){
            ex.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/changeAppState")
    public ResponseEntity changeAppState(@RequestBody AppState appState) {
        try{
            logger.info(String.format("Se recibe solicitud de cambiar el estado de la app en la propiedad %s del estado %s al estado %s",
                    appState.getPropertyID(),
                    appState.getOldState(),
                    appState.getNewState())
            );
            cbService.changeAppState(appState);
            return new ResponseEntity<>(HttpStatus.OK);
        }catch (Exception ex){
            ex.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/createProperty")
    public ResponseEntity<PropertyResponse> createProperty(@RequestBody Property property){
        try{
            return new ResponseEntity<>(cbService.createProperty(property), HttpStatus.OK);
        }catch (Exception ex){
            ex.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/invoice")
    public ResponseEntity invoice(@RequestBody InvoiceRequest invoiceRequest) {
        try{
            logger.info(String.format("Se recibe la factura %s del hotel %s", invoiceRequest.getInvoiceID(), invoiceRequest.getPropertyID()));
            cbService.invoice(invoiceRequest, false);
            return new ResponseEntity<>(HttpStatus.OK);
        }catch (Exception ex){
            ex.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/void-invoice")
    public ResponseEntity voidInvoice(@RequestBody InvoiceRequest invoiceRequest) {
        try{
            logger.info(String.format("Se recibe la factura %s del hotel %s", invoiceRequest.getInvoiceID(), invoiceRequest.getPropertyID()));
            cbService.invoice(invoiceRequest, true);
            return new ResponseEntity<>(HttpStatus.OK);
        }catch (Exception ex){
            ex.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
