package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import lombok.val;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static br.unb.cic.tdp.Application.CONFIGS_QUEUE;

@Component
public class Extensions {

    private final ProofStorage storage = new MySQLProofStorage("localhost", "luiz", "luiz");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = CONFIGS_QUEUE)
    public void listen(final String message) {
        val parts = message.split(";");
        val config = parts[0].split("#");
        val parent = parts[1].split("#");
        new SortOrExtendNew(rabbitTemplate, new Configuration(parent[0], parent[1]), new Configuration(config[0], config[1]), storage, 1.6).compute();
    }

    @EventListener
    public void onContextStarted(final ContextRefreshedEvent event) {
        rabbitTemplate.convertAndSend(CONFIGS_QUEUE, "(0 2 1)#(0 1 2);(0)#(0)");
    }
}