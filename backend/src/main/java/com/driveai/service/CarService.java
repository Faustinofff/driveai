package com.driveai.service;

import com.driveai.domain.model.Car;
import com.driveai.dto.car.CarWriteRequest;
import com.driveai.exception.ConflictException;
import com.driveai.exception.PreconditionFailedException;
import com.driveai.exception.ResourceNotFoundException;
import com.driveai.repository.CarRepository;
import com.driveai.specification.CarFilters;
import com.driveai.specification.CarSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CarService {

    public static final int MAX_PAGE_SIZE = 50;

    private final CarRepository carRepository;

    @Transactional(readOnly = true)
    public Page<Car> list(CarFilters filters, Pageable pageable) {
        Pageable limited = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
                pageable.getSort());
        return carRepository.findAll(CarSpecification.withFilters(filters), limited);
    }

    @Transactional(readOnly = true)
    public Car getById(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auto", id));
    }

    @Transactional
    public Car create(CarWriteRequest request) {
        Car car = Car.builder()
                .brand(request.brand().trim())
                .model(request.model().trim())
                .year(request.year())
                .price(request.price())
                .mileage(request.mileage())
                .fuelType(request.fuelType())
                .fuelConsumption(request.fuelConsumption())
                .transmission(request.transmission())
                .bodyType(request.bodyType())
                .imageUrl(request.imageUrl().trim())
                .description(request.description().trim())
                .stock(request.stock())
                .build();
        return carRepository.save(car);
    }

    @Transactional
    public Car update(Long id, CarWriteRequest request) {
        return update(id, request, null);
    }

    /**
     * Actualiza un auto aplicando concurrencia optimista si el cliente envía
     * {@code If-Match}. Un ETag desactualizado produce HTTP 412.
     */
    @Transactional
    public Car update(Long id, CarWriteRequest request, String ifMatch) {
        Car car = getById(id);
        if (ifMatch != null && !ifMatch.isBlank()) {
            String expected = normalizeEtag(ifMatch);
            String current = String.valueOf(car.getVersion() == null ? 0L : car.getVersion());
            if (!current.equals(expected)) {
                throw new PreconditionFailedException(
                        "El recurso fue modificado por otra operación. Recarga el auto e inténtalo de nuevo.");
            }
        }
        car.setBrand(request.brand().trim());
        car.setModel(request.model().trim());
        car.setYear(request.year());
        car.setPrice(request.price());
        car.setMileage(request.mileage());
        car.setFuelType(request.fuelType());
        car.setFuelConsumption(request.fuelConsumption());
        car.setTransmission(request.transmission());
        car.setBodyType(request.bodyType());
        car.setImageUrl(request.imageUrl().trim());
        car.setDescription(request.description().trim());
        car.setStock(request.stock());
        return carRepository.save(car);
    }

    private String normalizeEtag(String ifMatch) {
        String value = ifMatch.trim();
        if (value.startsWith("W/")) {
            value = value.substring(2).trim();
        }
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    @Transactional
    public void delete(Long id) {
        Car car = getById(id);
        try {
            carRepository.delete(car);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(
                    "El auto está referenciado en carritos u órdenes y no puede eliminarse.");
        }
    }
}