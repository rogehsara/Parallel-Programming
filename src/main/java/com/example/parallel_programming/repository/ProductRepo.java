package com.example.parallel_programming.repository;

import com.example.parallel_programming.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepo extends JpaRepository<Product, Long> {
}
