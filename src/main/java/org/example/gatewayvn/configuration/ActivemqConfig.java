package org.example.gatewayvn.configuration;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.SimpleJmsListenerContainerFactory;
import org.springframework.jms.core.JmsMessagingTemplate;

import javax.annotation.PostConstruct;
import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.jms.Topic;

@Slf4j
@Configuration
public class ActivemqConfig {
    @Value("${spring.activemq.broker-url}")
    private String brokerUrl;

    @Value("${spring.activemq.user}")
    private String username;

    @Value("${spring.activemq.password}")  // Corrected: password should be fetched
    private String password;

    @Value("${spring.activemq.queue-names:default-queue-name}")
    private String queueName;

    @Value("${spring.activemq.topic-name}")
    private String topicName;


//    @Value("${gateway.queue.target}")
//    private String targetQueue;

    @PostConstruct
    public void init() {
        log.info("ActiveMQ Queue Name: {}", queueName);
    }

    @Bean(name = "AOI_J10-1_4")
    public Queue AOI() {
        return new ActiveMQQueue("AOI_J10-1_4");
    }

    @Bean(name = "tianzunQueue")
    public Queue tianzunQueue() {
        return new ActiveMQQueue("tianzun_queue");
    }

    @Bean(name = "tianxingQueue")
    public Queue tianxingQueue() {
        return new ActiveMQQueue("tianxing_queue");
    }

    @Bean(name = "gzwdQueue")
    public Queue gzwdQueue() {
        return new ActiveMQQueue("gzwd_queue");
    }

    @Bean(name = "gzgtlQueue")
    public Queue gzgtlQueue() {
        return new ActiveMQQueue("gzgtl_queue");
    }

    @Bean(name = "jptcjQueue")
    public Queue jptcjQueue() {
        return new ActiveMQQueue("jptcj_queue");
    }

    @Bean(name = "jptymQueue")
    public Queue jptymQueue() {
        return new ActiveMQQueue("jptym_queue");
    }

    @Bean(name = "sdQueue")
    public Queue sdQueue() {
        return new ActiveMQQueue("sd_queue");
    }

    @Bean(name = "casiQueue")
    public Queue casiQueue() {
        return new ActiveMQQueue("casi_queue");
    }

    @Bean(name = "queue")
    public Queue queue() {
        return new ActiveMQQueue("zxc_test");
    }

    @Bean(name = "Report_ProductGague_J10-1_J10-1-4")
    public Queue reportBg() {
        return new ActiveMQQueue("Report_ProductGague_J10-1_J10-1-4");
    }

    @Bean(name = "Report_ProductGague_A9-3_A9-3")
    public Queue reportCg() {
        return new ActiveMQQueue("Report_ProductGague_A9-3_A9-3");
    }

    @Bean(name = "tkingQueue")
    public Queue tkingQueue() {
        return new ActiveMQQueue("tking_queue");
    }


    @Bean(name = "topic")
    public Topic topic() {
        return new ActiveMQTopic(topicName);
    }

    @Bean
    public ConnectionFactory connectionFactory(){
        return new ActiveMQConnectionFactory(username, password, brokerUrl);
    }

    @Bean
    public JmsMessagingTemplate jmsMessageTemplate(){
        return new JmsMessagingTemplate(connectionFactory());
    }

    // 在Queue模式中，对消息的监听需要对containerFactory进行配置
    @Bean("queueListener")
    public JmsListenerContainerFactory<?> queueJmsListenerContainerFactory(ConnectionFactory connectionFactory){
        log.debug("Creating JmsListenerContainerFactory url:{},userName:{},password:{},queueName,{},topicName:{}",brokerUrl,username,password,queueName,topicName);
        SimpleJmsListenerContainerFactory factory = new SimpleJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setPubSubDomain(false);
        return factory;
    }

    //在Topic模式中，对消息的监听需要对containerFactory进行配置
    @Bean("topicListener")
    public JmsListenerContainerFactory<?> topicJmsListenerContainerFactory(ConnectionFactory connectionFactory){
        log.debug("Creating JmsListenerContainerFactory url:{},userName:{},password:{},queueName,{},topicName:{}",brokerUrl,username,password,queueName,topicName);
        SimpleJmsListenerContainerFactory factory = new SimpleJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setPubSubDomain(true);
        return factory;
    }
}
