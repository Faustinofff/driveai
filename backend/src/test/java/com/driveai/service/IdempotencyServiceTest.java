package com.driveai.service;

import com.driveai.domain.model.IdempotencyRecord;
import com.driveai.exception.ConflictException;
import com.driveai.repository.IdempotencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    private static final Long USER_ID = 42L;

    @Mock
    private IdempotencyRepository repository;

    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(repository, new ObjectMapper());
    }

    @Test
    void withoutKeyRunsActionWithoutPersistence() {
        String result = service.execute(USER_ID, null, "body", String.class, () -> "ok");

        assertThat(result).isEqualTo("ok");
        verify(repository, never()).insertReservation(any(), anyString(), anyString());
    }

    @Test
    void blankKeyRunsActionWithoutPersistence() {
        String result = service.execute(USER_ID, "  ", "body", String.class, () -> "ok");

        assertThat(result).isEqualTo("ok");
        verify(repository, never()).insertReservation(any(), anyString(), anyString());
    }

    @Test
    void oversizedKeyIsRejected() {
        String key = "k".repeat(256);

        assertThatThrownBy(() -> service.execute(USER_ID, key, "body", String.class, () -> "ok"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("255");
    }

    @Test
    void firstRequestExecutesActionAndStoresResponse() {
        when(repository.insertReservation(eq(USER_ID), eq("key-1"), anyString())).thenReturn(1);
        AtomicInteger calls = new AtomicInteger();

        String result = service.execute(USER_ID, "key-1", "body", String.class, () -> {
            calls.incrementAndGet();
            return "created";
        });

        assertThat(result).isEqualTo("created");
        assertThat(calls).hasValue(1);
        verify(repository).complete(eq(USER_ID), eq("key-1"), eq(200), eq("\"created\""));
    }

    @Test
    void replayReturnsStoredResponseWithoutExecutingAction() {
        String hash = IdempotencyService.sha256("\"body\"");
        IdempotencyRecord record = IdempotencyRecord.builder()
                .userId(USER_ID)
                .idempotencyKey("key-2")
                .requestHash(hash)
                .statusCode(200)
                .responseBody("\"stored\"")
                .createdAt(Instant.now())
                .completedAt(Instant.now())
                .build();
        when(repository.insertReservation(eq(USER_ID), eq("key-2"), anyString())).thenReturn(0);
        when(repository.findByUserIdAndIdempotencyKey(USER_ID, "key-2")).thenReturn(Optional.of(record));
        AtomicInteger calls = new AtomicInteger();

        String result = service.execute(USER_ID, "key-2", "body", String.class, () -> {
            calls.incrementAndGet();
            return "new";
        });

        assertThat(result).isEqualTo("stored");
        assertThat(calls).hasValue(0);
    }

    @Test
    void reusingKeyWithDifferentPayloadThrowsConflict() {
        IdempotencyRecord record = IdempotencyRecord.builder()
                .userId(USER_ID)
                .idempotencyKey("key-3")
                .requestHash("another-hash")
                .responseBody("\"stored\"")
                .build();
        when(repository.insertReservation(eq(USER_ID), eq("key-3"), anyString())).thenReturn(0);
        when(repository.findByUserIdAndIdempotencyKey(USER_ID, "key-3")).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.execute(USER_ID, "key-3", "body", String.class, () -> "new"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cuerpo de solicitud distinto");
    }

    @Test
    void failedActionReleasesReservation() {
        when(repository.insertReservation(eq(USER_ID), eq("key-4"), anyString())).thenReturn(1);

        assertThatThrownBy(() -> service.execute(USER_ID, "key-4", "body", String.class, () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        verify(repository).deleteReservation(USER_ID, "key-4");
        verify(repository, never()).complete(any(), anyString(), anyInt(), anyString());
    }

    @Test
    void hashIsStableAndDifferencesAreDetected() {
        assertThat(IdempotencyService.sha256("abc"))
                .isEqualTo(IdempotencyService.sha256("abc"))
                .isNotEqualTo(IdempotencyService.sha256("abd"))
                .hasSize(64);
    }

    @Test
    void corruptedStoredResponseFailsFast() {
        String hash = IdempotencyService.sha256("\"body\"");
        IdempotencyRecord record = IdempotencyRecord.builder()
                .userId(USER_ID)
                .idempotencyKey("key-5")
                .requestHash(hash)
                .responseBody("{not-json")
                .build();
        when(repository.insertReservation(eq(USER_ID), eq("key-5"), anyString())).thenReturn(0);
        when(repository.findByUserIdAndIdempotencyKey(USER_ID, "key-5")).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.execute(USER_ID, "key-5", "body", String.class, () -> "new"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("corrupta");
    }

    @Test
    void concurrentRequestWithSameKeyWaitsThenConflicts() {
        String hash = IdempotencyService.sha256("\"body\"");
        IdempotencyRecord inProgress = IdempotencyRecord.builder()
                .userId(USER_ID)
                .idempotencyKey("key-6")
                .requestHash(hash)
                .responseBody(null)
                .build();
        when(repository.insertReservation(eq(USER_ID), eq("key-6"), anyString())).thenReturn(0);
        when(repository.findByUserIdAndIdempotencyKey(USER_ID, "key-6")).thenReturn(Optional.of(inProgress));

        assertThatThrownBy(() -> service.execute(USER_ID, "key-6", "body", String.class, () -> "new"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("solicitud en curso");
    }
}
