package com.andesstay.reservations.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {

    // Exchanges oficiales Caso AndesStay
    public static final String EXCHANGE_DIRECT = "cmd.direct";
    public static final String EXCHANGE_TOPIC = "cmd.topic";
    public static final String EXCHANGE_DLX = "cmd.dead.dlx";

    // Colas principales
    public static final String QUEUE_EMAIL = "q.cmd.email";
    public static final String QUEUE_HOUSEKEEPING = "q.cmd.housekeeping";
    public static final String QUEUE_VOUCHER = "q.cmd.voucher";

    // Dead Letter Queues (DLQ)
    public static final String QUEUE_EMAIL_DLQ = "q.cmd.email.dlq";
    public static final String QUEUE_HOUSEKEEPING_DLQ = "q.cmd.housekeeping.dlq";
    public static final String QUEUE_VOUCHER_DLQ = "q.cmd.voucher.dlq";

    // Routing keys directos
    public static final String ROUTING_KEY_EMAIL_SEND = "email.send";
    public static final String ROUTING_KEY_HOUSEKEEPING_TICKET = "housekeeping.ticket";
    public static final String ROUTING_KEY_VOUCHER_GEN = "voucher.gen";

    // Routing keys DLQ
    public static final String ROUTING_KEY_EMAIL_DLQ = "email.dlq";
    public static final String ROUTING_KEY_HOUSEKEEPING_DLQ = "housekeeping.dlq";
    public static final String ROUTING_KEY_VOUCHER_DLQ = "voucher.dlq";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    // Exchanges
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(EXCHANGE_DIRECT, true, false);
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE_TOPIC, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(EXCHANGE_DLX, true, false);
    }

    // DLQs
    @Bean
    public Queue emailDlq() {
        return QueueBuilder.durable(QUEUE_EMAIL_DLQ).build();
    }

    @Bean
    public Queue housekeepingDlq() {
        return QueueBuilder.durable(QUEUE_HOUSEKEEPING_DLQ).build();
    }

    @Bean
    public Queue voucherDlq() {
        return QueueBuilder.durable(QUEUE_VOUCHER_DLQ).build();
    }

    // Bindings de DLQs al Dead Letter Exchange
    @Bean
    public Binding bindingEmailDlq() {
        return BindingBuilder.bind(emailDlq()).to(deadLetterExchange()).with(ROUTING_KEY_EMAIL_DLQ);
    }

    @Bean
    public Binding bindingHousekeepingDlq() {
        return BindingBuilder.bind(housekeepingDlq()).to(deadLetterExchange()).with(ROUTING_KEY_HOUSEKEEPING_DLQ);
    }

    @Bean
    public Binding bindingVoucherDlq() {
        return BindingBuilder.bind(voucherDlq()).to(deadLetterExchange()).with(ROUTING_KEY_VOUCHER_DLQ);
    }

    // Colas Principales configuradas con Dead Letter Exchange
    @Bean
    public Queue emailQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", EXCHANGE_DLX);
        args.put("x-dead-letter-routing-key", ROUTING_KEY_EMAIL_DLQ);
        return QueueBuilder.durable(QUEUE_EMAIL).withArguments(args).build();
    }

    @Bean
    public Queue housekeepingQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", EXCHANGE_DLX);
        args.put("x-dead-letter-routing-key", ROUTING_KEY_HOUSEKEEPING_DLQ);
        return QueueBuilder.durable(QUEUE_HOUSEKEEPING).withArguments(args).build();
    }

    @Bean
    public Queue voucherQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", EXCHANGE_DLX);
        args.put("x-dead-letter-routing-key", ROUTING_KEY_VOUCHER_DLQ);
        return QueueBuilder.durable(QUEUE_VOUCHER).withArguments(args).build();
    }

    // Bindings directos
    @Bean
    public Binding bindingEmailDirect() {
        return BindingBuilder.bind(emailQueue()).to(directExchange()).with(ROUTING_KEY_EMAIL_SEND);
    }

    @Bean
    public Binding bindingHousekeepingDirect() {
        return BindingBuilder.bind(housekeepingQueue()).to(directExchange()).with(ROUTING_KEY_HOUSEKEEPING_TICKET);
    }

    @Bean
    public Binding bindingVoucherDirect() {
        return BindingBuilder.bind(voucherQueue()).to(directExchange()).with(ROUTING_KEY_VOUCHER_GEN);
    }

    // Bindings topic
    @Bean
    public Binding bindingEmailTopic() {
        return BindingBuilder.bind(emailQueue()).to(topicExchange()).with("email.*");
    }

    @Bean
    public Binding bindingHousekeepingTopic() {
        return BindingBuilder.bind(housekeepingQueue()).to(topicExchange()).with("housekeeping.#");
    }

    @Bean
    public Binding bindingVoucherTopic() {
        return BindingBuilder.bind(voucherQueue()).to(topicExchange()).with("voucher.*");
    }
}
