package com.agtech.cloudbedsbatchcr.facturacion.pa;

import com.agtech.cloudbedsbatchcr.facturacion.FacturaTransmisor;
import com.agtech.cloudbedsbatchcr.pojo.atvadapter.Invoice;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class TransmisorPAREST implements FacturaTransmisor {

    @Override
    public String transmitir(Invoice invoice, String queueName, String host, Integer port, String userName, String password, String virtualHost) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(userName);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);

        ObjectMapper mapper = new ObjectMapper();
        String jsonInvoice = mapper.writeValueAsString(invoice);

        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            channel.queueDeclare(queueName, true, false, false, null);
            channel.basicPublish("", queueName, null, jsonInvoice.getBytes());
        }

        return jsonInvoice;
    }
}

