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
import jakarta.transaction.Transactional;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ProductService {


    ProductRepo productRepo;
    StockEventProducer producer;
    OrderRepo orderRepo;
    TransactionService transactionService;
    RedissonClient redisson;
    Logger logger = LoggerFactory.getLogger(ProductService.class);
    private final ObjectMapper objectMapper;
    private static final String SUMMARY_FILE_PATH = "batch-summary.json";


    public ProductService(ProductRepo productRepo, StockEventProducer producer, OrderRepo orderRepo, TransactionService transactionService, RedissonClient redisson) {
        this.productRepo = productRepo;
        this.producer = producer;
        this.orderRepo = orderRepo;
        this.transactionService = transactionService;
        this.redisson = redisson;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }


    ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    @CachePut(value = "products", key = "#result.id")
    public Product addProduct(Product product) {
        return productRepo.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }

    @Cacheable(value = "products", key = "#id")
    public Optional<Product> getProductById(long id) {
        System.out.println("DB HIT for product " + id);
        return productRepo.findById(id);
    }

    @CacheEvict(value = "products", key = "#id")
    public void deleteProductById(long id) {
        productRepo.deleteById(id);
    }

    @CacheEvict(value = "products", key = "#updatedProduct.id")
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


    @CacheEvict(value = "products", key = "#id")
    public Product  decreaseProductQuantity(long id, int quantity) {

//        ReentrantLock lock = getLock(id);
        RLock lock = redisson.getLock("product:" + id);
        System.out.println(Thread.currentThread().getName());

        try {
            //Acquires the lock
            boolean acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);

            if (!acquired){
                throw new RuntimeException("could not acquire lock");
            }

//            lock.lock();
            return transactionService.decreaseProduct(id, quantity);


        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
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
