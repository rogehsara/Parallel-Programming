package com.example.parallel_programming.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;


@Getter
@Setter
@Table(name = "Order_record")
@Data
@Entity
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price")
    private double unitPrice;

    @Column(name = "total_price", nullable = false)
    private double totalPrice;

    @Column(name = "processed", nullable = false)
    private boolean processed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    public Order() {

    }

    public Order(int quantity, double unitPrice) {
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = quantity * unitPrice;
        this.processed = false;
        this.createdAt = LocalDateTime.now();
    }
}
