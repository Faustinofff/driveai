package com.driveai.specification;

import com.driveai.domain.enums.BodyType;
import com.driveai.domain.enums.FuelType;
import com.driveai.domain.model.Car;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarSpecificationTest {

    @Mock
    private CriteriaBuilder cb;
    @Mock
    private Root<Car> root;
    @Mock
    private Path<String> brandPath;
    @Mock
    private Path<String> modelPath;
    @Mock
    private Path<String> descriptionPath;
    @Mock
    private Path<BigDecimal> pricePath;
    @Mock
    private Path<Integer> yearPath;
    @Mock
    private Path<Object> fuelTypePath;
    @Mock
    private Path<Object> bodyTypePath;
    @Mock
    private Expression<String> lowerBrand;
    @Mock
    private Expression<String> lowerModel;
    @Mock
    private Expression<String> lowerDescription;
    @Mock
    private Predicate predicate;
    @Mock
    private Predicate conjunction;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        lenient().when(root.get("brand")).thenAnswer(inv -> brandPath);
        lenient().when(root.get("model")).thenAnswer(inv -> modelPath);
        lenient().when(root.get("description")).thenAnswer(inv -> descriptionPath);
        lenient().when(root.get("price")).thenAnswer(inv -> pricePath);
        lenient().when(root.get("year")).thenAnswer(inv -> yearPath);
        lenient().when(root.get("fuelType")).thenAnswer(inv -> fuelTypePath);
        lenient().when(root.get("bodyType")).thenAnswer(inv -> bodyTypePath);

        lenient().when(cb.lower(brandPath)).thenReturn(lowerBrand);
        lenient().when(cb.lower(modelPath)).thenReturn(lowerModel);
        lenient().when(cb.lower(descriptionPath)).thenReturn(lowerDescription);

        // Matchers tipados: evitan que el overload de CriteriaBuilder sea ambiguo.
        lenient().when(cb.equal(any(Expression.class), any(Object.class))).thenReturn(predicate);
        lenient().when(cb.greaterThanOrEqualTo(any(Expression.class), any(BigDecimal.class))).thenReturn(predicate);
        lenient().when(cb.lessThanOrEqualTo(any(Expression.class), any(BigDecimal.class))).thenReturn(predicate);
        lenient().when(cb.like(any(Expression.class), anyString())).thenReturn(predicate);
        lenient().when(cb.or(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
        lenient().when(cb.and(any(Predicate[].class))).thenReturn(conjunction);
    }

    @Test
    void emptyFiltersBuildsEmptyConjunction() {
        Specification<Car> spec = CarSpecification.withFilters(new CarFilters(null, null, null, null, null, null, null));

        Predicate result = spec.toPredicate(root, null, cb);

        assertThat(result).isSameAs(conjunction);
        verify(cb, never()).equal(any(Expression.class), any(Object.class));
        verify(cb, never()).greaterThanOrEqualTo(any(Expression.class), any(BigDecimal.class));
        verify(cb, never()).lessThanOrEqualTo(any(Expression.class), any(BigDecimal.class));
        verify(cb, never()).like(any(Expression.class), anyString());
    }

    @Test
    void combinedFiltersProduceOnePredicatePerAppliedFilter() {
        Specification<Car> spec = CarSpecification.withFilters(new CarFilters(
                "Toyota", new BigDecimal("20000"), new BigDecimal("30000"),
                2022, FuelType.GASOLINA, BodyType.SEDAN, null));

        spec.toPredicate(root, null, cb);

        ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(captor.capture());
        assertThat(captor.getValue()).hasSize(6);

        verify(cb).equal(lowerBrand, "toyota");
        verify(cb).greaterThanOrEqualTo(pricePath, new BigDecimal("20000"));
        verify(cb).lessThanOrEqualTo(pricePath, new BigDecimal("30000"));
        verify(cb).greaterThanOrEqualTo(yearPath, 2022);
    }

    @Test
    void searchTermBuildsLoweredLikeOverBrandModelAndDescription() {
        Specification<Car> spec = CarSpecification.withFilters(new CarFilters(null, null, null, null, null, null, "SUV"));

        spec.toPredicate(root, null, cb);

        ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(captor.capture());
        assertThat(captor.getValue()).hasSize(1);

        verify(cb).lower(brandPath);
        verify(cb).lower(modelPath);
        verify(cb).lower(descriptionPath);
        verify(cb, times(3)).like(any(Expression.class), eq("%suv%"));
    }

    @Test
    void fuelFilterUsesEnumValueDirectly() {
        Specification<Car> spec = CarSpecification.withFilters(new CarFilters(null, null, null, null, FuelType.DIESEL, null, null));

        spec.toPredicate(root, null, cb);

        ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        verify(cb).equal(fuelTypePath, FuelType.DIESEL);
    }
}