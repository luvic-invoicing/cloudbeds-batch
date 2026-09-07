package com.agtech.cloudbedsbatchcr.services;

import com.agtech.cloudbedsbatchcr.pojo.atvadapter.Invoice;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class RabbitInvoicePublisher {

    private final Environment env;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RabbitInvoicePublisher(Environment env) {
        this.env = env;
    }

    public String publish(Invoice invoice) throws Exception {
        String queueName = env.getProperty("atvadapter.rabbitmq.queue");
        String host = env.getProperty("atvadapter.rabbitmq.server");
        Integer port = Integer.parseInt(env.getProperty("atvadapter.rabbitmq.port", "5672"));
        String userName = env.getProperty("atvadapter.rabbitmq.userName");
        String password = env.getProperty("atvadapter.rabbitmq.password");
        String virtualHost = env.getProperty("atvadapter.rabbitmq.virtualHost");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(userName);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);

        String jsonInvoice = objectMapper.writeValueAsString(invoice);

        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            channel.queueDeclare(queueName, true, false, false, null);
            channel.basicPublish("", queueName, null, jsonInvoice.getBytes());
        }

        return jsonInvoice;
    }
}

