package com.driveai.controller;

import com.driveai.domain.enums.BodyType;
import com.driveai.domain.enums.FuelType;
import com.driveai.domain.model.Car;
import com.driveai.dto.car.CarResponse;
import com.driveai.dto.car.CarWriteRequest;
import com.driveai.dto.common.PageResponse;
import com.driveai.service.CarService;
import com.driveai.specification.CarFilters;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @Operation(summary = "Publica un auto nuevo (solo ADMIN)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CarResponse> create(@Valid @RequestBody CarWriteRequest request) {
        Car car = carService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CarResponse.from(car));
    }

    @Operation(summary = "Actualiza un auto existente (solo ADMIN)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CarResponse update(@PathVariable Long id, @Valid @RequestBody CarWriteRequest request) {
        return CarResponse.from(carService.update(id, request));
    }

    @Operation(summary = "Elimina un auto (solo ADMIN; 409 si está en uso)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        carService.delete(id);
        return ResponseEntity.noContent().build();
    }
}