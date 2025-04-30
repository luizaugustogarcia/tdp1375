package br.unb.cic.tdp;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@EnableRabbit
public class Application {
    public static final String CONFIGS_QUEUE = "configs";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public Queue configsQueue() {
        return new Queue(CONFIGS_QUEUE, true);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReadyEvent() {
        //rabbitTemplate.convertAndSend(CONFIGS_QUEUE, "(0 2 1)#(0 1 2)");
    }
}
