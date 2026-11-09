package com.driveai.dto.car;

import com.driveai.domain.enums.BodyType;
import com.driveai.domain.enums.FuelType;
import com.driveai.domain.enums.Transmission;
import com.driveai.domain.model.Car;

import java.math.BigDecimal;

public record CarResponse(
        Long id,
        String brand,
        String model,
        Integer year,
        BigDecimal price,
        Integer mileage,
        FuelType fuelType,
        BigDecimal fuelConsumption,
        Transmission transmission,
        BodyType bodyType,
        String imageUrl,
        String description,
        Integer stock,
        Long version) {

    public static CarResponse from(Car car) {
        return new CarResponse(
                car.getId(),
                car.getBrand(),
                car.getModel(),
                car.getYear(),
                car.getPrice(),
                car.getMileage(),
                car.getFuelType(),
                car.getFuelConsumption(),
                car.getTransmission(),
                car.getBodyType(),
                car.getImageUrl(),
                car.getDescription(),
                car.getStock(),
                car.getVersion());
    }
}
