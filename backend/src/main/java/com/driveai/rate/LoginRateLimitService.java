package com.driveai.rate;

import com.driveai.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter específico de login para mitigar fuerza bruta.
 *
 * <p>Aplica dos cuotas independientes:
 * <ul>
 *   <li><b>IP + email</b>: corta el ataque típico desde un mismo origen.</li>
 *   <li><b>Solo email (por cuenta)</b>: corta el ataque que rota la IP o falsifica
 *       {@code X-Forwarded-For}. Sin esta cuota, cambiar la IP en cada intento
 *       creaba un bucket nuevo y permitía intentos ilimitados contra una cuenta
 *       (defecto DEF-07).</li>
 * </ul>
 * Al exceder cualquiera de las dos se responde HTTP 429 con {@code Retry-After}.
 */
@Component
public class LoginRateLimitService {

    private static final int MAX_BUCKETS = 20_000;
    private static final int DEFAULT_ACCOUNT_MULTIPLIER = 3;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int requestsPerMinute;
    private final int accountRequestsPerMinute;

    /** Constructor de conveniencia (tests): la cuota por cuenta es 3 veces la de IP + email. */
    public LoginRateLimitService(int requestsPerMinute) {
        this(requestsPerMinute, Math.max(1, requestsPerMinute) * DEFAULT_ACCOUNT_MULTIPLIER);
    }

    @Autowired
    public LoginRateLimitService(
            @Value("${app.login-rate-limit.requests-per-minute:5}") int requestsPerMinute,
            @Value("${app.login-rate-limit.per-account-per-minute:15}") int accountRequestsPerMinute) {
        this.requestsPerMinute = Math.max(1, requestsPerMinute);
        this.accountRequestsPerMinute = Math.max(this.requestsPerMinute, accountRequestsPerMinute);
    }

    public void check(String clientIp, String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String ipKey = "ip|" + (clientIp == null ? "unknown" : clientIp) + "|" + normalizedEmail;
        String accountKey = "acct|" + normalizedEmail;

        Bucket ipBucket = buckets.computeIfAbsent(ipKey, ignored -> newBucket(requestsPerMinute));
        if (!ipBucket.tryConsume(1)) {
            throw new RateLimitExceededException(retryAfterSeconds(requestsPerMinute));
        }
        Bucket accountBucket = buckets.computeIfAbsent(accountKey,
                ignored -> newBucket(accountRequestsPerMinute));
        if (!accountBucket.tryConsume(1)) {
            throw new RateLimitExceededException(retryAfterSeconds(accountRequestsPerMinute));
        }
        evictIfNeeded();
    }

    private static Bucket newBucket(int perMinute) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(
                        perMinute,
                        Refill.greedy(perMinute, Duration.ofMinutes(1))))
                .build();
    }

    private static long retryAfterSeconds(int perMinute) {
        return Math.max(1L, (long) Math.ceil(60.0 / perMinute));
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
