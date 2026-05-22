package com.example.parallel_programming.messaging;


import com.example.parallel_programming.configuration.RabbitMQConfig;
import com.example.parallel_programming.dto.StockDecreaseMsg;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class StockEventProducer {

    final RabbitTemplate rabbitTemplate;


    public StockEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendStockEvent(long productId, int quantity) {
        StockDecreaseMsg msg = new StockDecreaseMsg(productId, quantity);

        rabbitTemplate.convertAndSend(RabbitMQConfig.STOCK_QUEUE, msg);
    }
}
