package com.driveai.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessedMessageServiceTest {

    @Mock
    private com.driveai.repository.ProcessedMessageRepository repository;

    @InjectMocks
    private ProcessedMessageService service;

    @Test
    void firstDeliveryIsMarkedAsNew() {
        when(repository.insertIfAbsent("evt-1", "consumer")).thenReturn(1);

        assertThat(service.markIfFirst("evt-1", "consumer")).isTrue();
    }

    @Test
    void duplicateDeliveryIsDetected() {
        when(repository.insertIfAbsent("evt-1", "consumer")).thenReturn(0);

        assertThat(service.markIfFirst("evt-1", "consumer")).isFalse();
    }

    @Test
    void missingMessageIdIsRejected() {
        assertThatThrownBy(() -> service.markIfFirst(" ", "consumer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messageId");
    }
}
