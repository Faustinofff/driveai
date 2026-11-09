package com.driveai.repository;

import com.driveai.domain.model.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {

    /**
     * Inserción atómica e idempotente. Devuelve 1 si el mensaje se marcó por
     * primera vez y 0 si ya existía (duplicado).
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO processed_messages (message_id, consumer)
            VALUES (:messageId, :consumer)
            ON CONFLICT (message_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("messageId") String messageId, @Param("consumer") String consumer);
}
