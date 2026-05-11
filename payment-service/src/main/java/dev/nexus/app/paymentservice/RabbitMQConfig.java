package dev.nexus.app.paymentservice;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "payment_exchange";

    // Routing Keys
    public static final String SUCCESS_ROUTING_KEY = "payment.success";
    public static final String FAILED_ROUTING_KEY = "payment.failed";

    // Queues
    public static final String SUCCESS_QUEUE = "payment_success_queue";
    public static final String FAILED_QUEUE = "payment_failed_queue";

    @Bean
    public Queue successQueue() {
        return new Queue(SUCCESS_QUEUE);
    }

    @Bean
    public Queue failedQueue() {
        return new Queue(FAILED_QUEUE);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    // Bind Success Queue
    @Bean
    public Binding successBinding(Queue successQueue, TopicExchange exchange) {
        return BindingBuilder.bind(successQueue).to(exchange).with(SUCCESS_ROUTING_KEY);
    }

    // Bind Failed Queue
    @Bean
    public Binding failedBinding(Queue failedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(failedQueue).to(exchange).with(FAILED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter converter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public AmqpTemplate template(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }
}