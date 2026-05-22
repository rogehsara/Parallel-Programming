package com.example.parallel_programming.messaging;

import com.example.parallel_programming.configuration.RabbitMQConfig;
import com.example.parallel_programming.dto.StockDecreaseMsg;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class StockEventConsumer {

    Logger logger = Logger.getLogger(StockEventConsumer.class.getName());

    @RabbitListener(queues = RabbitMQConfig.STOCK_QUEUE)
    public void handleStockDecrease(StockDecreaseMsg msg){
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
