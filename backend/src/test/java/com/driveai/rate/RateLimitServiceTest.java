package com.driveai.rate;

import com.driveai.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitServiceTest {

    @Test
    void blocksRequestAfterCapacityIsExhausted() {
        RateLimitService service = new RateLimitService(2);

        service.check("key-a");
        service.check("key-a");

        assertThatThrownBy(() -> service.check("key-a"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(ex -> assertThatCode(() ->
                        ((RateLimitExceededException) ex).getRetryAfterSeconds()).doesNotThrowAnyException());
    }

    @Test
    void keysAreIsolatedFromEachOther() {
        RateLimitService service = new RateLimitService(1);

        service.check("user-1");
        assertThatCode(() -> service.check("user-2")).doesNotThrowAnyException();

        assertThatThrownBy(() -> service.check("user-2"))
                .isInstanceOf(RateLimitExceededException.class);
    }
}