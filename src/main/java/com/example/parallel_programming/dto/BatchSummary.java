package com.example.parallel_programming.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BatchSummary {

    private int totalOrders;
    private int totalQuantity;
    private double totalRevenue;
    private LocalDateTime lastUpdated;


}