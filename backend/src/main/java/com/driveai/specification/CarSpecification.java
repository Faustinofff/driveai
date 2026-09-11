package com.driveai.specification;

import com.driveai.domain.model.Car;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Construye dinámicamente el {@code Predicate} de búsqueda sobre el catálogo.
 * Soporta filtros por marca, rango de precio, año mínimo, tipo de combustible,
 * tipo de carrocería y término libre (brand/model/description).
 */
public final class CarSpecification {

    private CarSpecification() {
    }

    public static Specification<Car> withFilters(CarFilters filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(filters.brand())) {
                predicates.add(cb.equal(cb.lower(root.get("brand")),
                        filters.brand().trim().toLowerCase(Locale.ROOT)));
            }
            if (filters.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filters.minPrice()));
            }
            if (filters.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filters.maxPrice()));
            }
            if (filters.minYear() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("year"), filters.minYear()));
            }
            if (filters.fuelType() != null) {
                predicates.add(cb.equal(root.get("fuelType"), filters.fuelType()));
            }
            if (filters.bodyType() != null) {
                predicates.add(cb.equal(root.get("bodyType"), filters.bodyType()));
            }
            if (StringUtils.hasText(filters.q())) {
                predicates.add(searchPredicate(root, cb, filters.q()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate searchPredicate(
            jakarta.persistence.criteria.Root<Car> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            String term) {
        String pattern = "%" + term.trim().toLowerCase(Locale.ROOT) + "%";
        Predicate brandMatch = cb.like(cb.lower(root.get("brand")), pattern);
        Predicate modelMatch = cb.like(cb.lower(root.get("model")), pattern);
        Predicate descriptionMatch = cb.like(cb.lower(root.get("description")), pattern);
        Predicate search = cb.or(brandMatch, modelMatch);
        return cb.or(search, descriptionMatch);
    }
}