/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.agtech.cloudbedsbatchcr.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

/**
 * @author Hugo
 */

@Configuration
public class RabbitConfig implements RabbitListenerConfigurer
{

    public static final String CB_NOTIFICATION_QUEUE = "cb-notification-queue";

    public static final String EXCHANGE_INBOUNDS = "cb-notification-exchange";

    @Bean
    Queue cbNotificationQueue()
    {
        return QueueBuilder.durable( CB_NOTIFICATION_QUEUE ).build();
    }

    @Bean
    Exchange cbNotificationExchange()
    {
        return ExchangeBuilder.topicExchange( EXCHANGE_INBOUNDS ).build();
    }

    @Bean
    Binding binding( Queue cbNotificationQueue, TopicExchange cbNotificationExchange )
    {
        return BindingBuilder.bind( cbNotificationQueue ).to( cbNotificationExchange ).with( CB_NOTIFICATION_QUEUE );
    }

    @Override
    public void configureRabbitListeners( RabbitListenerEndpointRegistrar registrar )
    {
        registrar.setMessageHandlerMethodFactory( messageHandlerMethodFactory() );
    }

    @Bean
    MessageHandlerMethodFactory messageHandlerMethodFactory()
    {
        DefaultMessageHandlerMethodFactory messageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();
        messageHandlerMethodFactory.setMessageConverter( consumerJackson2MessageConverter() );
        return messageHandlerMethodFactory;
    }

    @Bean
    public MappingJackson2MessageConverter consumerJackson2MessageConverter()
    {
        return new MappingJackson2MessageConverter();
    }

}