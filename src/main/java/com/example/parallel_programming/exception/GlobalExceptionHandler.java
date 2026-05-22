package com.example.parallel_programming.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidStockException.class)
    public ResponseEntity<?> handleInvalidStockException(InvalidStockException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        Map.of(
                        "time Stamp", LocalTime.now(),
                        "error" , "Invalid Stock Operation",
                        "message", e.getMessage()
                        )
                );
    }
}
