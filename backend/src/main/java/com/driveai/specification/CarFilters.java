package com.driveai.specification;

import com.driveai.domain.enums.BodyType;
import com.driveai.domain.enums.FuelType;

import java.math.BigDecimal;

/**
 * Criterios de filtrado dinámico del catálogo.
 * El controlador traduce los query params a este record y el
 * {@link CarSpecification} lo convierte en un {@code Predicate} JPA.
 */
public record CarFilters(
        String brand,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minYear,
        FuelType fuelType,
        BodyType bodyType,
        String q) {
}