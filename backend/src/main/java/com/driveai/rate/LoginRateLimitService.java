package com.driveai.rate;

import com.driveai.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter específico de login para mitigar fuerza bruta.
 *
 * <p>La clave combina IP + email normalizado, de modo que un atacante no pueda
 * probar contraseñas ilimitadas contra una cuenta (ni barrer muchas cuentas
 * desde una misma IP sin agotar su cuota). Al exceder el límite se responde
 * HTTP 429 con {@code Retry-After}.
 */
@Component
public class LoginRateLimitService {

    private static final int MAX_BUCKETS = 20_000;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int requestsPerMinute;

    public LoginRateLimitService(
            @Value("${app.login-rate-limit.requests-per-minute:5}") int requestsPerMinute) {
        this.requestsPerMinute = Math.max(1, requestsPerMinute);
    }

    public void check(String clientIp, String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String key = (clientIp == null ? "unknown" : clientIp) + "|" + normalizedEmail;
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> newBucket());
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException(retryAfterSeconds());
        }
        evictIfNeeded();
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(
                        requestsPerMinute,
                        Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))))
                .build();
    }

    private long retryAfterSeconds() {
        return Math.max(1L, (long) Math.ceil(60.0 / requestsPerMinute));
    }

    private void evictIfNeeded() {
        if (buckets.size() > MAX_BUCKETS) {
            String victim = buckets.keySet().iterator().next();
            buckets.remove(victim);
        }
    }

    @PreDestroy
    public void shutdown() {
        buckets.clear();
    }
}
