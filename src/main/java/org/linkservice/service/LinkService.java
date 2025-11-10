package org.linkservice.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.linkservice.controller.dto.CreateShortLinkRequest;
import org.linkservice.controller.dto.CreateShortLinkResponse;
import org.linkservice.model.ShortLink;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class LinkService {
    @Value("${click-limit}")
    private int clickLimit;
    @Value("${ttl-seconds}")
    private int ttlSeconds;
    @Value("${code-length}")
    private int codeLength;

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /**
     * userId -> (shortCode -> ShortLink)
     */
    private final Map<UUID, Map<String, ShortLink>> userLinks = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final NotificationService notificationService;

    public CreateShortLinkResponse createShortLink(CreateShortLinkRequest request) {
        UUID userId = request.getUuid() != null ? request.getUuid() : UUID.randomUUID();;
        String longUrl = request.getLongUrl();

        Map<String, ShortLink> links = userLinks.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());

        Optional<Map.Entry<String, ShortLink>> existing = links.entrySet().stream()
                .filter(e -> e.getValue().getLongUrl().equals(longUrl))
                .findFirst();

        String shortCode = existing
                .map(Map.Entry::getKey)
                .orElseGet(this::generateShortCode);

        links.putIfAbsent(shortCode, new ShortLink(
                longUrl,
                Instant.now(),
                clickLimit
        ));

        CreateShortLinkResponse response = new CreateShortLinkResponse();
        response.setShortUrl(shortCode);
        response.setUuid(userId);
        return response;
    }

    public Optional<String> getOriginalUrl(UUID userId, String shortCode) {
        Map<String, ShortLink> links = userLinks.get(userId);
        if (links == null) return Optional.empty();

        ShortLink link = links.get(shortCode);
        if (link == null) return Optional.empty();

        // Проверяем TTL
        long ageSeconds = Instant.now().getEpochSecond() - link.getCreatedAt().getEpochSecond();
        if (ageSeconds > ttlSeconds) {
            links.remove(shortCode);
            // в реальности уведомление прихранивалось бы в бд/очередь
            notificationService.notifyUser(userId, "Срок жизни ссылки истёк: " + shortCode);
            return Optional.empty();
        }

        // Проверяем лимит кликов
        if (link.getRemainingClicks() <= 0) {
            links.remove(shortCode);
            notificationService.notifyUser(userId, "Срок действия ссылки истёк по лимиту переходов: " + shortCode);
            return Optional.empty();
        }

        link.setRemainingClicks(link.getRemainingClicks() - 1);
        return Optional.of(link.getLongUrl());
    }

    private String generateShortCode() {
        StringBuilder sb = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
