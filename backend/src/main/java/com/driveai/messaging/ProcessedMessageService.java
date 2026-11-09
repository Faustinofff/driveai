package com.driveai.messaging;

import com.driveai.repository.ProcessedMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Marca mensajes procesados de forma atómica. Devuelve {@code true} solo la
 * primera vez que se ve un messageId.
 */
@Service
@RequiredArgsConstructor
public class ProcessedMessageService {

    private final ProcessedMessageRepository repository;

    public boolean markIfFirst(String messageId, String consumer) {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("messageId es obligatorio");
        }
        return repository.insertIfAbsent(messageId, consumer) == 1;
    }
}
