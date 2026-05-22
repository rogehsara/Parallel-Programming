package com.example.parallel_programming.exception;

public class InvalidStockException extends RuntimeException{
    public InvalidStockException(String message) {
        super(message);
    }
}
