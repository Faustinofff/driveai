package com.driveai.service;

import com.driveai.domain.enums.BodyType;
import com.driveai.domain.enums.FuelType;
import com.driveai.domain.enums.Transmission;
import com.driveai.domain.model.Car;
import com.driveai.domain.model.Cart;
import com.driveai.domain.model.CartItem;
import com.driveai.dto.cart.CartAddRequest;
import com.driveai.dto.cart.CartResponse;
import com.driveai.exception.InsufficientStockException;
import com.driveai.exception.ResourceNotFoundException;
import com.driveai.repository.CarRepository;
import com.driveai.repository.CartItemRepository;
import com.driveai.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final Long USER_ID = 42L;

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void getCartReturnsEmptyCartWhenUserHasNoCart() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        CartResponse response = cartService.getCart(USER_ID);

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.items()).isEmpty();
        assertThat(response.subtotal()).isEqualByComparingTo("0");
    }

    @Test
    void addToCartCreatesCartWithPriceSnapshot() {
        Car car = car(7L, 5, "22500.00");
        when(carRepository.findById(7L)).thenReturn(Optional.of(car));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.addToCart(new CartAddRequest(7L, 2), USER_ID);

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(2);
        assertThat(response.items().get(0).priceSnapshot()).isEqualByComparingTo("22500.00");
        assertThat(response.subtotal()).isEqualByComparingTo("45000.00");
    }

    @Test
    void addToCartAccumulatesQuantityForSameCar() {
        Car car = car(7L, 10, "100.00");
        Cart cart = new Cart();
        cart.setUserId(USER_ID);
        CartItem item = new CartItem();
        item.setCar(car);
        item.setQuantity(3);
        item.setPriceSnapshot(car.getPrice());
        cart.addItem(item);
        when(carRepository.findById(7L)).thenReturn(Optional.of(car));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.addToCart(new CartAddRequest(7L, 2), USER_ID);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(5);
        assertThat(response.subtotal()).isEqualByComparingTo("500.00");
    }

    @Test
    void addToCartRejectsQuantityAboveStock() {
        Car car = car(7L, 2, "100.00");
        when(carRepository.findById(7L)).thenReturn(Optional.of(car));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart(new CartAddRequest(7L, 3), USER_ID))
                .isInstanceOf(InsufficientStockException.class);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCartWithUnknownCarThrowsNotFound() {
        when(carRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart(new CartAddRequest(99L, 1), USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateItemOnlyWorksForOwnedItem() {
        Car car = car(7L, 10, "100.00");
        CartItem item = new CartItem();
        item.setId(5L);
        item.setCar(car);
        item.setQuantity(1);
        item.setPriceSnapshot(car.getPrice());
        Cart cart = new Cart();
        cart.setUserId(USER_ID);
        cart.addItem(item);

        when(cartItemRepository.findByIdAndCart_UserId(5L, USER_ID)).thenReturn(Optional.of(item));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.updateItem(5L, USER_ID, 4);

        assertThat(response.items().get(0).quantity()).isEqualTo(4);
        verify(cartItemRepository).save(item);
    }

    @Test
    void updateItemOfAnotherUserIsNotFound() {
        when(cartItemRepository.findByIdAndCart_UserId(5L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItem(5L, USER_ID, 2))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateItemRejectsQuantityAboveStock() {
        Car car = car(7L, 2, "100.00");
        CartItem item = new CartItem();
        item.setId(5L);
        item.setCar(car);
        item.setQuantity(1);
        when(cartItemRepository.findByIdAndCart_UserId(5L, USER_ID)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.updateItem(5L, USER_ID, 3))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void removeItemDeletesOnlyOwnedItem() {
        Car car = car(7L, 10, "100.00");
        CartItem item = new CartItem();
        item.setId(5L);
        item.setCar(car);
        item.setQuantity(1);
        item.setPriceSnapshot(car.getPrice());
        Cart cart = new Cart();
        cart.setUserId(USER_ID);
        cart.addItem(item);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.removeItem(5L, USER_ID);

        assertThat(response.items()).isEmpty();
    }

    @Test
    void removeUnknownItemThrowsNotFound() {
        Cart cart = new Cart();
        cart.setUserId(USER_ID);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.removeItem(5L, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void clearCartDeletesOwnCartOnly() {
        Cart cart = new Cart();
        cart.setUserId(USER_ID);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        cartService.clearCart(USER_ID);

        verify(cartRepository).delete(cart);
    }

    @Test
    void clearCartWithoutCartDoesNothing() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        cartService.clearCart(USER_ID);

        verify(cartRepository, never()).delete(any());
    }

    private static Car car(Long id, int stock, String price) {
        return Car.builder()
                .id(id)
                .brand("Toyota")
                .model("Corolla")
                .year(2022)
                .price(new BigDecimal(price))
                .mileage(10000)
                .fuelType(FuelType.GASOLINA)
                .fuelConsumption(new BigDecimal("5.80"))
                .transmission(Transmission.AUTOMATICA)
                .bodyType(BodyType.SEDAN)
                .imageUrl("https://img.test/car.jpg")
                .description("Auto de prueba")
                .stock(stock)
                .build();
    }
}
