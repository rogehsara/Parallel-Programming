package com.example.parallel_programming.batchProcessing;

import com.example.parallel_programming.dto.BatchSummary;
import com.example.parallel_programming.entity.Order;
import com.example.parallel_programming.repository.OrderRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ProcessOrderBatch {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS");
    private static final Logger logger = LoggerFactory.getLogger(ProcessOrderBatch.class);

    private static final String SUMMARY_FILE_PATH = "batch-summary.json";

    private final OrderRepo repo;
    private final ObjectMapper objectMapper;

    public ProcessOrderBatch(OrderRepo repo) {
        this.repo = repo;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Scheduled(fixedRate = 60000)
    public void processOrderBatch() {
        logger.info("[BATCH PROCESSING] Processing order batch... | date and time: {}",
                LocalDateTime.now().format(formatter)
        );

        List<Order> batch = repo.findTop5ByProcessedFalseOrderByCreatedAtDesc();

        if (batch.isEmpty()) {
            logger.info("[BATCH PROCESSING] No orders found!");
            return;
        }

        int batchOrders = batch.size();

        int batchQuantity = batch.stream()
                .mapToInt(Order::getQuantity)
                .sum();

        double batchRevenue = batch.stream()
                .mapToDouble(Order::getTotalPrice)
                .sum();


        batch.forEach(order -> order.setProcessed(true));
        repo.saveAll(batch);

        logger.info(
                "[BATCH PROCESSING] Batch processed | orders={} | quantity={} | revenue={}",
                batchOrders,
                batchQuantity,
                batchRevenue
        );

        updateSummaryFile(batchOrders, batchQuantity, batchRevenue);

    }

    private synchronized void updateSummaryFile(int batchOrders, int batchQuantity, double batchRevenue) {
        try {
            BatchSummary summary = readSummary();

            summary.setTotalOrders(summary.getTotalOrders() + batchOrders);
            summary.setTotalQuantity(summary.getTotalQuantity() + batchQuantity);
            summary.setTotalRevenue(summary.getTotalRevenue() + batchRevenue);
            summary.setLastUpdated(LocalDateTime.now());

            objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(new File(SUMMARY_FILE_PATH), summary);

            logger.info(
                    "[BATCH SUMMARY] updated | totalOrders={} | totalQuantity={} | totalRevenue={}",
                    summary.getTotalOrders(),
                    summary.getTotalQuantity(),
                    summary.getTotalRevenue()
            );

        } catch (Exception e) {
            logger.error("[BATCH SUMMARY] failed to update summary file | error={}", e.getMessage());
        }
    }

    private BatchSummary readSummary() {
        try {
            File file = new File(SUMMARY_FILE_PATH);

            if (!file.exists()) {
                return new BatchSummary(0, 0, 0.0, null);
            }

            return objectMapper.readValue(file, BatchSummary.class);

        } catch (Exception e) {
            logger.error("[BATCH SUMMARY] failed to read summary file, creating new one | error={}", e.getMessage());
            return new BatchSummary(0, 0, 0.0, null);
        }
    }
}