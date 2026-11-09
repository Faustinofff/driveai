package com.driveai.rate;

import com.driveai.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimitServiceTest {

    @Test
    void blocksBruteForceAfterConfiguredAttempts() {
        LoginRateLimitService service = new LoginRateLimitService(2);

        service.check("10.0.0.1", "ana@driveai.test");
        service.check("10.0.0.1", "ana@driveai.test");

        assertThatThrownBy(() -> service.check("10.0.0.1", "ana@driveai.test"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(ex -> assertThat(((RateLimitExceededException) ex).getRetryAfterSeconds())
                        .isGreaterThanOrEqualTo(1L));
    }

    @Test
    void bucketsAreIsolatedByIpAndEmail() {
        LoginRateLimitService service = new LoginRateLimitService(1);

        service.check("10.0.0.1", "ana@driveai.test");

        assertThatCode(() -> service.check("10.0.0.2", "ana@driveai.test"))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.check("10.0.0.1", "beto@driveai.test"))
                .doesNotThrowAnyException();
    }

    @Test
    void emailIsNormalizedForBucketing() {
        LoginRateLimitService service = new LoginRateLimitService(1);

        service.check("10.0.0.1", "Ana@DriveAI.test");

        assertThatThrownBy(() -> service.check("10.0.0.1", "  ana@driveai.TEST "))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void nullIpAndEmailAreHandled() {
        LoginRateLimitService service = new LoginRateLimitService(1);

        service.check(null, null);

        assertThatThrownBy(() -> service.check(null, null))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void minimumCapacityIsOneEvenWithInvalidConfiguration() {
        LoginRateLimitService service = new LoginRateLimitService(0);

        service.check("ip", "a@b.test");

        assertThatThrownBy(() -> service.check("ip", "a@b.test"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void shutdownClearsBuckets() {
        LoginRateLimitService service = new LoginRateLimitService(1);
        service.check("ip", "a@b.test");

        service.shutdown();

        assertThatCode(() -> service.check("ip", "a@b.test")).doesNotThrowAnyException();
    }
}
