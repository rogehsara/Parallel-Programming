package com.example.parallel_programming.services;

import com.example.parallel_programming.entity.Order;
import com.example.parallel_programming.entity.Product;
import com.example.parallel_programming.exception.InvalidStockException;
import com.example.parallel_programming.messaging.StockEventProducer;
import com.example.parallel_programming.repository.OrderRepo;
import com.example.parallel_programming.repository.ProductRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    ProductRepo productRepo;
    OrderRepo orderRepo;
    StockEventProducer producer;


    public TransactionService(ProductRepo productRepo, OrderRepo orderRepo, StockEventProducer producer) {
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
        this.producer = producer;
    }
   @Transactional
    public Product decreaseProduct(long id, int quantity){
        Product existingProduct =
                productRepo.findById(id).orElseThrow(
                        () -> new RuntimeException("Product not found")
                );
        int existingQuantity = existingProduct.getQuantity();

        System.out.println(
                "Before = " + existingQuantity
        );

        if (existingQuantity < quantity) {
            throw new InvalidStockException("The product is out of stock");
        }
        existingProduct.setQuantity(existingQuantity - quantity);


        producer.sendStockEvent(id, quantity);
        Order order = new Order(quantity, existingProduct.getPrice());
        order.setProduct(existingProduct);
        orderRepo.save(order);



        Product newProduct = productRepo.save(existingProduct);

     if(true) {
            throw new RuntimeException();
        }

        return newProduct;

    }

}
