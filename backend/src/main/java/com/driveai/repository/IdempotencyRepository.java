package com.driveai.repository;

import com.driveai.domain.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    /**
     * Reserva atómica. Devuelve 1 si esta ejecución creó la clave y 0 si ya
     * existía (solicitud concurrente o reintento).
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO idempotency_keys (user_id, idempotency_key, request_hash)
            VALUES (:userId, :key, :hash)
            ON CONFLICT (user_id, idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertReservation(@Param("userId") Long userId,
                          @Param("key") String key,
                          @Param("hash") String hash);

    @Modifying
    @Transactional
    @Query("""
            UPDATE IdempotencyRecord r
               SET r.statusCode = :statusCode,
                   r.responseBody = :responseBody,
                   r.completedAt = CURRENT_TIMESTAMP
             WHERE r.userId = :userId AND r.idempotencyKey = :key
            """)
    int complete(@Param("userId") Long userId,
                 @Param("key") String key,
                 @Param("statusCode") Integer statusCode,
                 @Param("responseBody") String responseBody);

    @Modifying
    @Transactional
    @Query("DELETE FROM IdempotencyRecord r WHERE r.userId = :userId AND r.idempotencyKey = :key")
    int deleteReservation(@Param("userId") Long userId, @Param("key") String key);
}
