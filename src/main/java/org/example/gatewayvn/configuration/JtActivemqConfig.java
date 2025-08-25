package org.example.gatewayvn.configuration;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.SimpleJmsListenerContainerFactory;
import org.springframework.jms.core.JmsMessagingTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;

@Slf4j
@Configuration
public class JtActivemqConfig {

    @Value("${spring.activemqjt.broker-url}")
    private String brokerUrl;

    @Value("${spring.activemqjt.user}")
    private String username;

    @Value("${spring.activemqjt.password}")
    private String password;

    @Value("${spring.activemqjt.queue-name}")
    private String queueName;

    @Bean(name = "jtConnectionFactory")
    public ConnectionFactory jtConnectionFactory() {
        return new ActiveMQConnectionFactory(username, password, brokerUrl);
    }

    @Bean(name = "jtJmsMessagingTemplate")
    public JmsMessagingTemplate jtJmsMessagingTemplate(@Qualifier("jtConnectionFactory") ConnectionFactory jtConnectionFactory) {
        return new JmsMessagingTemplate(jtConnectionFactory);
    }

    @Bean(name = "jtQueue")
    public Queue jtQueue() {
        return new ActiveMQQueue(queueName);
    }

    @Bean(name = "jtQueueListener")
    public JmsListenerContainerFactory<?> jtQueueJmsListenerContainerFactory(@Qualifier("jtConnectionFactory") ConnectionFactory connectionFactory) {
        SimpleJmsListenerContainerFactory factory = new SimpleJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setPubSubDomain(false);
        return factory;
    }
}
