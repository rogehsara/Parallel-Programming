package com.example.parallel_programming.aspect;

import com.example.parallel_programming.dto.StockDecreaseMsg;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.slf4j.LoggerFactory;

@Aspect
@Component
public class Logging {

    @Value("${server.port}")
    private String port;

    Logger logger = LoggerFactory.getLogger(Logging.class);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS");

    @Around("execution(* com.example.parallel_programming.services.ProductService.*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {



        String requestId = UUID.randomUUID().toString();
        String methodName = joinPoint.getSignature().getName();
        long startNano = System.nanoTime();
        String startTime = LocalDateTime.now().format(formatter);

        try {
            Object result = joinPoint.proceed();

            long endNano = System.nanoTime();
            String endTime = LocalDateTime.now().format(formatter);
            printMethodSuccess(requestId, methodName, startTime, endTime, endNano, startNano);

            return result;

        } catch (Throwable t) {

            long endNano = System.nanoTime();
            String endTime = LocalDateTime.now().format(formatter);

            printMethodFailed(t, requestId, methodName, startTime, endTime, endNano, startNano);
            throw t;
        }
    }

    @Around("execution(* com.example.parallel_programming.messaging.StockEventConsumer.*(..))")
    public Object logMessagingConsumerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        StockDecreaseMsg msg = (StockDecreaseMsg) joinPoint.getArgs()[0];

        logger.info("[RABBITMQ] Processing background task | productId: {} | quantity: {} | date and time: {} | processed on port: {}",
                msg.getProductId(),
                msg.getQuantity(),
                LocalDateTime.now().format(formatter),
                port
        );

        joinPoint.proceed();

        logger.info("[RABBITMQ CONSUMER] Background task finished for productId: {} | date and time: {} | processed on port: {}",
                msg.getProductId(),
                LocalDateTime.now().format(formatter),
                port
                );

        return null;
    }


    @Around("execution(* com.example.parallel_programming.messaging.StockEventProducer.*(..))")
    public Object logMessagingProducerMethods(ProceedingJoinPoint joinPoint) throws Throwable{
        long productId = (Long)joinPoint.getArgs()[0];
        int quantity = (Integer)joinPoint.getArgs()[1];


        joinPoint.proceed();

        logger.info(" [RABBITMQ PRODUCER] Message sent to RabbitMQ | productId: {} | quantity: {} | date and time: {} | processed on port: {}",
                productId,
                quantity,
                LocalDateTime.now().format(formatter),
                port
                );
        return null;
    }

    private synchronized void printMethodFailed(
            Throwable e,
            String requestId,
            String methodName,
            String startTime,
            String endTime,
            long endNano,
            long startNano
    ) {

        long durationMs = (endNano - startNano) / 1_000_000;

        logger.error(
                "[ERROR] method failed | requestId={} | method={} | startedAt={} | endedAt={} | durationMs={} | error={} | processed on port: {}",
                requestId,
                methodName,
                startTime,
                endTime,
                durationMs,
                e.getMessage(),
                port
        );
    }

    private synchronized void printMethodSuccess(
            String requestId,
            String methodName,
            String startTime,
            String endTime,
            long endNano,
            long startNano
    ) {

        long durationMs = (endNano - startNano) / 1_000_000;

        logger.info(
                "[SERVICE] order completed | requestId={} | method={} | startedAt={} | endedAt={} | durationMs={} | processed on port: {}",
                requestId,
                methodName,
                startTime,
                endTime,
                durationMs,
                port
        );
    }
}
