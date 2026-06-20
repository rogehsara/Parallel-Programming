package com.example.parallel_programming.entity;


import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "Products")
@Entity
public class Product implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(
            name = "quantity",
            nullable = false,
            check = {
                @CheckConstraint(name = "quantity_not_below_zero", constraint = "quantity >= 0")
                }
            )
    private int quantity;


    @Column(name = "price", nullable = false)
    private double price;
}
