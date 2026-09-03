package com.agtech.cloudbedsbatchcr.facturacion;

import com.agtech.cloudbedsbatchcr.pojo.atvadapter.Invoice;

public interface FacturaTransmisor {
    String transmitir(Invoice invoice, String queueName, String host, Integer port, String userName, String password, String virtualHost) throws Exception;
}

