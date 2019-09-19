//package com.hoffi.minimal.microservices.microservice.bootconfigs;
//
//import java.util.Map;
//
//import org.springframework.amqp.core.AmqpAdmin;
//import org.springframework.amqp.core.Queue;
//import org.springframework.amqp.core.QueueBuilder;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.cloud.stream.config.BindingProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Profile;
//
//@Configuration
//public class RabbitQueuesConfig {
//
//    private Map<String, BindingProperties> bindingProperties;
//
//    /** you have to add a RabbitAdmin to your config to auto-declared queues, exchanges, bindings. */
//    @Autowired
//    AmqpAdmin amqpAdmin;
//
//    @Profile("tier1")
//    @Configuration
//    class RabbitQueuesTier1 {
//        @Bean
//        public Queue tier1Queue() {
//            return QueueBuilder.durable("minimal-SourceTo1").withArgument("x-max-length", "10").build();
//        }
//    }
//
//    @Profile("tier2")
//    @Configuration
//    class RabbitQueuesTier2 {
//        @Bean
//        public Queue tier2Queue() {
//            return QueueBuilder.durable("minimal-1To2").withArgument("x-max-length", "10").build();
//        }
//    }
//
//    @Profile("sink")
//    @Configuration
//    class RabbitQueuesSink {
//        @Bean
//        public Queue sinkQueue() {
//            return QueueBuilder.durable("minimal-2ToSink").withArgument("x-max-length", "10").build();
//        }
//    }
//
//}
