package org.linkservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.linkservice.controller.dto.CreateShortLinkRequest;
import org.linkservice.controller.dto.CreateShortLinkResponse;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LinkServiceTest {

    private NotificationService notificationService;
    private LinkService linkService;

    private final int CLICK_LIMIT = 2;
    private final int TTL_SECONDS = 1_000;
    private final int CODE_LENGTH = 6;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);

        linkService = new LinkService(notificationService);
        setField(linkService, "clickLimit", CLICK_LIMIT);
        setField(linkService, "ttlSeconds", TTL_SECONDS);
        setField(linkService, "codeLength", CODE_LENGTH);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createShortLink_shouldGenerateShortLink() {
        CreateShortLinkRequest request = new CreateShortLinkRequest();
        request.setUuid(UUID.randomUUID());
        request.setLongUrl("https://example.com");

        CreateShortLinkResponse response = linkService.createShortLink(request);

        assertNotNull(response);
        assertNotNull(response.getShortUrl());
        assertEquals(request.getUuid(), response.getUuid());
        assertEquals(CODE_LENGTH, response.getShortUrl().length());
    }

    @Test
    void getOriginalUrl_shouldReturnUrlAndDecreaseClicks() {
        UUID userId = UUID.randomUUID();
        String longUrl = "https://example.com";

        CreateShortLinkRequest request = new CreateShortLinkRequest();
        request.setUuid(userId);
        request.setLongUrl(longUrl);

        String shortCode = linkService.createShortLink(request).getShortUrl();

        Optional<String> first = linkService.getOriginalUrl(userId, shortCode);
        Optional<String> second = linkService.getOriginalUrl(userId, shortCode);
        Optional<String> third = linkService.getOriginalUrl(userId, shortCode); // должен сработать лимит

        assertTrue(first.isPresent());
        assertEquals(longUrl, first.get());

        assertTrue(second.isPresent());
        assertEquals(longUrl, second.get());

        assertTrue(third.isEmpty());

        // Проверка, что уведомление о лимите вызвано
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifyUser(eq(userId), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("лимиту переходов"));
    }

    @Test
    void createShortLink_shouldGenerateUuidIfNull() {
        CreateShortLinkRequest request = new CreateShortLinkRequest();
        request.setLongUrl("https://example.com");

        CreateShortLinkResponse response = linkService.createShortLink(request);

        assertNotNull(response.getUuid());
        assertNotNull(response.getShortUrl());
    }
}
