package dev.nexus.app.notificationservice;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Tells the Listener how to read the JSON letters (using Jackson 3)
    @Bean
    public MessageConverter converter() {
        return new JacksonJsonMessageConverter();
    }
}