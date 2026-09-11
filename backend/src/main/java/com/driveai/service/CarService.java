package com.driveai.service;

import com.driveai.domain.model.Car;
import com.driveai.exception.ResourceNotFoundException;
import com.driveai.repository.CarRepository;
import com.driveai.specification.CarFilters;
import com.driveai.specification.CarSpecification;
import lombok.RequiredArgsConstructor;
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
}