package com.driveai.rate;

import com.driveai.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter local con Bucket4j. Cada key (sessionId|IP combinados) obtiene un
 * bucket independiente con {@code N} requests por minuto. Al exceder el límite
 * se lanza {@link RateLimitExceededException} (HTTP 429).
 */
@Component
public class RateLimitService {

    private static final int MAX_BUCKETS = 10_000;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int requestsPerMinute;

    public RateLimitService(@Value("${app.rate-limit.requests-per-minute:5}") int requestsPerMinute) {
        this.requestsPerMinute = Math.max(1, requestsPerMinute);
    }

    public void check(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, this::newBucket);
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException(retryAfterSeconds());
        }
        evictIfNeeded();
    }

    private Bucket newBucket(String key) {
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
}