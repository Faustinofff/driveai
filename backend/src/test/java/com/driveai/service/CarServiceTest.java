package com.driveai.service;

import com.driveai.domain.enums.BodyType;
import com.driveai.domain.enums.FuelType;
import com.driveai.domain.enums.Transmission;
import com.driveai.domain.model.Car;
import com.driveai.dto.car.CarWriteRequest;
import com.driveai.exception.ConflictException;
import com.driveai.exception.PreconditionFailedException;
import com.driveai.exception.ResourceNotFoundException;
import com.driveai.repository.CarRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private CarService carService;

    private CarWriteRequest request() {
        return new CarWriteRequest(
                "Mazda", "3 Sport", 2024,
                new BigDecimal("26000.00"), 8000, FuelType.GASOLINA,
                new BigDecimal("6.2"), Transmission.AUTOMATICA,
                BodyType.HATCHBACK, "https://img.test/mazda.jpg",
                "Auto de prueba para panel admin.", 6);
    }

    @Test
    void createPersistsANewCar() {
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Car created = carService.create(request());

        assertThat(created.getBrand()).isEqualTo("Mazda");
        assertThat(created.getStock()).isEqualTo(6);
    }

    @Test
    void updateAppliesChanges() {
        Car existing = Car.builder().id(9L).brand("Mazda").model("3").year(2020).build();
        when(carRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Car updated = carService.update(9L, request());

        assertThat(updated.getModel()).isEqualTo("3 Sport");
        assertThat(updated.getYear()).isEqualTo(2024);
        assertThat(updated.getPrice()).isEqualByComparingTo("26000.00");
    }

    @Test
    void updateWhenMissingThrowsNotFound() {
        when(carRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> carService.update(99L, request()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateWithMatchingIfMatchSucceeds() {
        Car existing = Car.builder().id(9L).brand("Mazda").model("3").year(2020).version(3L).build();
        when(carRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Car updated = carService.update(9L, request(), "\"3\"");

        assertThat(updated.getModel()).isEqualTo("3 Sport");
    }

    @Test
    void updateWithWeakEtagSucceeds() {
        Car existing = Car.builder().id(9L).brand("Mazda").model("3").year(2020).version(3L).build();
        when(carRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Car updated = carService.update(9L, request(), "W/\"3\"");

        assertThat(updated.getYear()).isEqualTo(2024);
    }

    @Test
    void updateWithStaleIfMatchThrowsPreconditionFailed() {
        Car existing = Car.builder().id(9L).brand("Mazda").model("3").year(2020).version(3L).build();
        when(carRepository.findById(9L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> carService.update(9L, request(), "\"2\""))
                .isInstanceOf(PreconditionFailedException.class)
                .hasMessageContaining("modificado");
        verify(carRepository, never()).save(any(Car.class));
    }

    @Test
    void deleteRemovesTheCar() {
        Car existing = Car.builder().id(9L).brand("Mazda").model("3").year(2020).build();
        when(carRepository.findById(9L)).thenReturn(Optional.of(existing));

        carService.delete(9L);

        verify(carRepository).delete(existing);
        verify(carRepository).flush();
    }

    /** DEF-09: la violación de FK aparece al hacer flush, no al llamar a delete(). */
    @Test
    void deleteWhenFkViolationSurfacesOnFlushThrowsConflict() {
        Car existing = Car.builder().id(9L).brand("Mazda").model("3").year(2020).build();
        when(carRepository.findById(9L)).thenReturn(Optional.of(existing));
        doThrow(new DataIntegrityViolationException("fk")).when(carRepository).flush();

        assertThatThrownBy(() -> carService.delete(9L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("referenciado");
    }

    @Test
    void deleteWhenReferencedThrowsConflict() {
        Car existing = Car.builder().id(9L).brand("Mazda").model("3").year(2020).build();
        when(carRepository.findById(9L)).thenReturn(Optional.of(existing));
        doThrow(new DataIntegrityViolationException("fk")).when(carRepository).delete(any(Car.class));

        assertThatThrownBy(() -> carService.delete(9L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("referenciado");
    }
}