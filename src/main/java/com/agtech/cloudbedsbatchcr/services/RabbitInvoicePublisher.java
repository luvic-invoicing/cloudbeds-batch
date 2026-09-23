package com.agtech.cloudbedsbatchcr.services;

import com.agtech.cloudbedsbatchcr.pojo.atvadapter.Invoice;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class RabbitInvoicePublisher {

    private final Environment env;
    private final ConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RabbitInvoicePublisher(Environment env, ConnectionFactory connectionFactory) {
        this.env = env;
        this.connectionFactory = connectionFactory;
    }

    public String publish(Invoice invoice) throws Exception {
        String queueName = env.getProperty("atvadapter.rabbitmq.queue");

        String jsonInvoice = objectMapper.writeValueAsString(invoice);

        try (org.springframework.amqp.rabbit.connection.Connection connection = connectionFactory.createConnection();
             Channel channel = connection.createChannel(false)) {
            channel.queueDeclare(queueName, true, false, false, null);
            channel.basicPublish("", queueName, null, jsonInvoice.getBytes());
        }

        return jsonInvoice;
    }
}

