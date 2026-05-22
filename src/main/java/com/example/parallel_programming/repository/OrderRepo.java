package com.example.parallel_programming.repository;

import com.example.parallel_programming.entity.Order;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface OrderRepo extends CrudRepository<Order, Long> {

    public List<Order> findTop5ByProcessedFalseOrderByCreatedAtDesc();

}
