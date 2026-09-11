package com.driveai.controller;

import com.driveai.domain.enums.BodyType;
import com.driveai.domain.enums.FuelType;
import com.driveai.domain.model.Car;
import com.driveai.dto.car.CarResponse;
import com.driveai.dto.common.PageResponse;
import com.driveai.service.CarService;
import com.driveai.specification.CarFilters;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
@Tag(name = "Catálogo", description = "Consulta paginada y filtrable del catálogo de autos")
public class CarController {

    private final CarService carService;

    @Operation(summary = "Lista autos con paginación y filtros dinámicos")
    @GetMapping
    public PageResponse<CarResponse> list(
            @PageableDefault(size = 12) Pageable pageable,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minYear,
            @RequestParam(required = false) FuelType fuelType,
            @RequestParam(required = false) BodyType bodyType,
            @RequestParam(required = false) String q) {

        CarFilters filters = new CarFilters(brand, minPrice, maxPrice, minYear, fuelType, bodyType, q);
        Page<Car> page = carService.list(filters, pageable);
        return PageResponse.from(page.map(CarResponse::from));
    }

    @Operation(summary = "Obtiene un auto por id (404 si no existe)")
    @GetMapping("/{id}")
    public CarResponse detail(@PathVariable Long id) {
        return CarResponse.from(carService.getById(id));
    }
}