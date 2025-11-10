package org.linkservice.service;

import java.util.UUID;

public interface NotificationService {
    void notifyUser(UUID userId, String message);
}
