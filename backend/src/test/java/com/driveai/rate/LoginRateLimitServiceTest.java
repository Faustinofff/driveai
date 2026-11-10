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

    /**
     * DEF-07: rotar la IP (o falsificar X-Forwarded-For) no debe dar intentos
     * ilimitados contra una misma cuenta. La cuota por cuenta corta el ataque.
     */
    @Test
    void rotatingIpsAgainstSameAccountIsBlockedByAccountQuota() {
        LoginRateLimitService service = new LoginRateLimitService(1, 3);

        service.check("1.1.1.1", "victima@driveai.test");
        service.check("2.2.2.2", "victima@driveai.test");
        service.check("3.3.3.3", "victima@driveai.test");

        assertThatThrownBy(() -> service.check("4.4.4.4", "victima@driveai.test"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(ex -> assertThat(((RateLimitExceededException) ex).getRetryAfterSeconds())
                        .isEqualTo(20L));
        // La cuota por cuenta no afecta a otras cuentas.
        assertThatCode(() -> service.check("4.4.4.4", "otra@driveai.test"))
                .doesNotThrowAnyException();
    }

    @Test
    void accountQuotaIsNeverLowerThanIpQuota() {
        LoginRateLimitService service = new LoginRateLimitService(2, 1);

        service.check("1.1.1.1", "ana@driveai.test");

        assertThatCode(() -> service.check("2.2.2.2", "ana@driveai.test"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> service.check("3.3.3.3", "ana@driveai.test"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void defaultAccountQuotaIsThreeTimesIpQuota() {
        LoginRateLimitService service = new LoginRateLimitService(1);

        service.check("1.1.1.1", "ana@driveai.test");
        service.check("2.2.2.2", "ana@driveai.test");
        service.check("3.3.3.3", "ana@driveai.test");

        assertThatThrownBy(() -> service.check("4.4.4.4", "ana@driveai.test"))
                .isInstanceOf(RateLimitExceededException.class);
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
