package com.example.parallel_programming.services;

import com.example.parallel_programming.dto.BatchSummary;
import com.example.parallel_programming.entity.Order;
import com.example.parallel_programming.entity.Product;
import com.example.parallel_programming.exception.InvalidStockException;
import com.example.parallel_programming.messaging.StockEventProducer;
import com.example.parallel_programming.repository.OrderRepo;
import com.example.parallel_programming.repository.ProductRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ProductService {


    ProductRepo productRepo;
    StockEventProducer producer;
    OrderRepo orderRepo;
    Logger logger = LoggerFactory.getLogger(ProductService.class);
    private final ObjectMapper objectMapper;
    private static final String SUMMARY_FILE_PATH = "batch-summary.json";


    public ProductService(ProductRepo productRepo, StockEventProducer producer, OrderRepo orderRepo) {
        this.productRepo = productRepo;
        this.producer = producer;
        this.orderRepo = orderRepo;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }


    ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    public Product addProduct(Product product) {
        return productRepo.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }

    public Optional<Product> getProductById(long id) {
        return productRepo.findById(id);
    }

    public void deleteProductById(long id) {
        productRepo.deleteById(id);
    }

    public Product updateProduct(Product updatedProduct) {
        Product existingProduct =
                productRepo.findById(updatedProduct.getId()).orElseThrow(
                        () -> new RuntimeException("Product not found")
                );
        existingProduct.setName(updatedProduct.getName());
        existingProduct.setDescription(updatedProduct.getDescription());
        existingProduct.setQuantity(updatedProduct.getQuantity());
        return productRepo.save(existingProduct);
    }


    public Product decreaseProductQuantity(long id, int quantity) {

        ReentrantLock lock = getLock(id);
        System.out.println(Thread.currentThread().getName());

        try {
            //Acquires the lock
//            lock.lock();




            Product existingProduct =
                productRepo.findById(id).orElseThrow(
                        () -> new RuntimeException("Product not found")
                );
            int existingQuantity = existingProduct.getQuantity();
            if (existingQuantity < quantity) {
                throw new InvalidStockException("The product is out of stock");
            }
            existingProduct.setQuantity(existingQuantity - quantity);


            producer.sendStockEvent(id, quantity);

//            noQueue(existingProduct);


            Order order = new Order(quantity, existingProduct.getPrice());
            order.setProduct(existingProduct);
            orderRepo.save(order);

//            updateSummaryFile(1, quantity, (quantity * existingProduct.getPrice()));
//            order.setProcessed(true);


            
            return productRepo.save(existingProduct);
        }
        finally {
//            lock.unlock();
        }
    }

    private void noQueue(Product existingProduct) {
        logger.info("[WITHOUT RABBITMQ] Processing task | productId: {} | quantity: {}",
                existingProduct.getId(),
                existingProduct.getQuantity()
        );

        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }


        logger.info("[WITHOUT RABBITMQ]  task finished for productId: {}",
                existingProduct.getId()
        );
    }


    private ReentrantLock getLock(Long productId) {
        return locks.computeIfAbsent(productId, id -> new ReentrantLock());
    }

    @Async("customExecutor")
    public void decreaseStockAsync(Long id, int amount) {
        decreaseProductQuantity(id, amount);
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
                    "[WITH NO BATCH -DIRECT- -SEQUENTIAL- ] updated | totalOrders={} | totalQuantity={} | totalRevenue={}",
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
