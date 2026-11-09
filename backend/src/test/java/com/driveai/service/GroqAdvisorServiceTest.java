package com.driveai.service;

import com.driveai.ai.GroqApiClient;
import com.driveai.domain.enums.BodyType;
import com.driveai.domain.enums.FuelType;
import com.driveai.domain.enums.Transmission;
import com.driveai.domain.model.AiRecommendationLog;
import com.driveai.domain.model.Car;
import com.driveai.dto.ai.AiRecommendResponse;
import com.driveai.exception.GroqApiException;
import com.driveai.rate.RateLimitService;
import com.driveai.repository.AiRecommendationLogRepository;
import com.driveai.repository.CarRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroqAdvisorServiceTest {

    @Mock
    private GroqApiClient groqApiClient;
    @Mock
    private CarRepository carRepository;
    @Mock
    private AiRecommendationLogRepository logRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RateLimitService rateLimitService = new RateLimitService(100);

    private GroqAdvisorService advisorService;

    @BeforeEach
    void setUp() {
        advisorService = new GroqAdvisorService(
                groqApiClient, carRepository, logRepository, rateLimitService, objectMapper);
    }

    @Test
    void filtersHallucinatedIdsAndKeepsOnlyExistingInStockCars() {
        Car real = car(3L, 4);
        when(carRepository.findByStockGreaterThan(0)).thenReturn(List.of(real));
        when(groqApiClient.chatCompletion(anyString(), anyString()))
                .thenReturn("{\"reasoning\":\"Excelente equilibrio precio-eficiencia\",\"recommendedCarIds\":[3,999,12345]}");
        when(carRepository.findAllById(List.of(3L, 999L, 12345L))).thenReturn(List.of(real));

        AiRecommendResponse response = advisorService.recommend(
                "Quiero un auto económico", "session-1", "session-1|127.0.0.1");

        assertThat(response.recommendations()).hasSize(1);
        assertThat(response.recommendations().get(0).id()).isEqualTo(3L);
        assertThat(response.reasoning()).isEqualTo("Excelente equilibrio precio-eficiencia");
        verify(logRepository).save(any(AiRecommendationLog.class));
    }

    @Test
    void dropsOutOfStockCarsFromRecommendations() {
        Car outOfStock = car(5L, 0);
        Car inStock = car(7L, 2);
        when(carRepository.findByStockGreaterThan(0)).thenReturn(List.of(outOfStock, inStock));
        when(groqApiClient.chatCompletion(anyString(), anyString()))
                .thenReturn("{\"reasoning\":\"La segunda está disponible\",\"recommendedCarIds\":[5,7]}");
        when(carRepository.findAllById(List.of(5L, 7L))).thenReturn(List.of(outOfStock, inStock));

        AiRecommendResponse response = advisorService.recommend(
                "Recomiéndame algo", "session-2", "session-2|127.0.0.1");

        assertThat(response.recommendations()).extracting(item -> item.id()).containsExactly(7L);
    }

    @Test
    void corruptedJsonResponseIsConvertedToFriendlyGroqExceptionAndLogged() {
        when(carRepository.findByStockGreaterThan(0)).thenReturn(List.of());
        when(groqApiClient.chatCompletion(anyString(), anyString()))
                .thenReturn("{esto-no-es-json}");

        assertThatThrownBy(() -> advisorService.recommend("Hola", "session-3", "session-3|127.0.0.1"))
                .isInstanceOf(GroqApiException.class)
                .hasMessageContaining("respuesta inválida");

        verify(logRepository).save(any(AiRecommendationLog.class));
    }

    @Test
    void groqSaturationPropagatesFriendlyMessageAndLogsError() {
        when(carRepository.findByStockGreaterThan(0)).thenReturn(List.of());
        when(groqApiClient.chatCompletion(anyString(), anyString()))
                .thenThrow(new GroqApiException("El servicio de IA está saturado en este momento. Inténtalo de nuevo en unos segundos."));

        assertThatThrownBy(() -> advisorService.recommend("Quiero un SUV", "session-4", "session-4|127.0.0.1"))
                .isInstanceOf(GroqApiException.class)
                .hasMessageContaining("saturado");

        verify(logRepository).save(any(AiRecommendationLog.class));
    }

    @Test
    void emptyRecommendedIdsReturnsEmptyRecommendationsButStillLogs() {
        when(carRepository.findByStockGreaterThan(0)).thenReturn(List.of(car(1L, 1)));
        when(groqApiClient.chatCompletion(anyString(), anyString()))
                .thenReturn("{\"reasoning\":\"Nada encaja con tu consulta.\",\"recommendedCarIds\":[]}");

        AiRecommendResponse response = advisorService.recommend(
                "Un auto amarillo", "session-5", "session-5|127.0.0.1");

        assertThat(response.recommendations()).isEmpty();
        assertThat(response.reasoning()).isEqualTo("Nada encaja con tu consulta.");
        verify(logRepository).save(any(AiRecommendationLog.class));
    }

    @Test
    void textualNumericIdsAreAcceptedAndInvalidStringsDropped() {
        Car real = car(3L, 4);
        when(carRepository.findByStockGreaterThan(0)).thenReturn(List.of(real));
        when(groqApiClient.chatCompletion(anyString(), anyString()))
                .thenReturn("{\"reasoning\":\"ok\",\"recommendedCarIds\":[\"3\",\"abc\",null]}");
        when(carRepository.findAllById(List.of(3L))).thenReturn(List.of(real));

        AiRecommendResponse response = advisorService.recommend("auto", "s", "s|ip");

        assertThat(response.recommendations()).extracting(item -> item.id()).containsExactly(3L);
    }

    @Test
    void duplicatesAreCollapsedAndAtMostFiveCarsAreRecommended() {
        List<Car> cars = List.of(
                car(1L, 1), car(2L, 1), car(3L, 1), car(4L, 1), car(5L, 1), car(6L, 1));
        when(carRepository.findByStockGreaterThan(0)).thenReturn(cars);
        when(groqApiClient.chatCompletion(anyString(), anyString()))
                .thenReturn("{\"reasoning\":\"ok\",\"recommendedCarIds\":[1,1,2,3,4,5,6]}");
        when(carRepository.findAllById(List.of(1L, 2L, 3L, 4L, 5L))).thenReturn(cars);

        AiRecommendResponse response = advisorService.recommend("auto", "s", "s|ip");

        assertThat(response.recommendations()).hasSize(5);
        verify(carRepository).findAllById(List.of(1L, 2L, 3L, 4L, 5L));
    }

    @Test
    void nonObjectJsonIsRejected() {
        when(carRepository.findByStockGreaterThan(0)).thenReturn(List.of());
        when(groqApiClient.chatCompletion(anyString(), anyString())).thenReturn("[1,2,3]");

        assertThatThrownBy(() -> advisorService.recommend("auto", "s", "s|ip"))
                .isInstanceOf(GroqApiException.class)
                .hasMessageContaining("respuesta inválida");
    }

    @Test
    void missingRecommendedIdsIsRejected() {
        when(carRepository.findByStockGreaterThan(0)).thenReturn(List.of());
        when(groqApiClient.chatCompletion(anyString(), anyString()))
                .thenReturn("{\"reasoning\":\"ok\"}");

        assertThatThrownBy(() -> advisorService.recommend("auto", "s", "s|ip"))
                .isInstanceOf(GroqApiException.class)
                .hasMessageContaining("recomendaciones válidas");
    }

    @Test
    void reasoningMayBeMissing() {
        when(carRepository.findByStockGreaterThan(0)).thenReturn(List.of());
        when(groqApiClient.chatCompletion(anyString(), anyString()))
                .thenReturn("{\"recommendedCarIds\":[]}");

        AiRecommendResponse response = advisorService.recommend("auto", "s", "s|ip");

        assertThat(response.reasoning()).isNull();
    }

    private static Car car(Long id, int stock) {
        return Car.builder()
                .id(id)
                .brand("Toyota")
                .model("Corolla")
                .year(2022)
                .price(new BigDecimal("22500.00"))
                .mileage(10000)
                .fuelType(FuelType.GASOLINA)
                .fuelConsumption(new BigDecimal("5.80"))
                .transmission(Transmission.AUTOMATICA)
                .bodyType(BodyType.SEDAN)
                .imageUrl("https://images.unsplash.com/photo-test.jpg")
                .description("Auto de prueba")
                .stock(stock)
                .build();
    }
}