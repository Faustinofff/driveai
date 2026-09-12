package com.driveai.dto.car;

import com.driveai.domain.enums.BodyType;
import com.driveai.domain.enums.FuelType;
import com.driveai.domain.enums.Transmission;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CarWriteRequest(
        @NotBlank(message = "La marca es obligatoria")
        @Size(max = 100, message = "La marca no puede superar 100 caracteres")
        String brand,

        @NotBlank(message = "El modelo es obligatorio")
        @Size(max = 100, message = "El modelo no puede superar 100 caracteres")
        String model,

        @NotNull(message = "El año es obligatorio")
        Integer year,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
        BigDecimal price,

        @NotNull(message = "El kilometraje es obligatorio")
        @Min(value = 0, message = "El kilometraje no puede ser negativo")
        Integer mileage,

        @NotNull(message = "El tipo de combustible es obligatorio")
        FuelType fuelType,

        @NotNull(message = "El consumo es obligatorio")
        @DecimalMin(value = "0.0", message = "El consumo no puede ser negativo")
        BigDecimal fuelConsumption,

        @NotNull(message = "La transmisión es obligatoria")
        Transmission transmission,

        @NotNull(message = "La carrocería es obligatoria")
        BodyType bodyType,

        @NotBlank(message = "La URL de la imagen es obligatoria")
        @Size(max = 2000, message = "La URL no puede superar 2000 caracteres")
        String imageUrl,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String description,

        @NotNull(message = "El stock es obligatorio")
        @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stock) {
}