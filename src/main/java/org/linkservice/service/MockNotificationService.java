package org.linkservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class MockNotificationService implements NotificationService {

    @Override
    public void notifyUser(UUID userId, String message) {
        log.info("[Notification] Пользователь {}: {}", userId, message);
    }
}