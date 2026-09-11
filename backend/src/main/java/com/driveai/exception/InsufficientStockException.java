package com.driveai.exception;

import lombok.Getter;

@Getter
public class InsufficientStockException extends RuntimeException {

    private final Long carId;
    private final Integer requested;
    private final Integer available;

    public InsufficientStockException(Long carId, Integer requested, Integer available) {
        super("Stock insuficiente para el auto " + carId
                + ". Disponibles: " + available + ", solicitados: " + requested + ".");
        this.carId = carId;
        this.requested = requested;
        this.available = available;
    }
}