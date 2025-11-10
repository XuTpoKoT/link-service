package org.linkservice.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.linkservice.controller.dto.CreateShortLinkRequest;
import org.linkservice.controller.dto.CreateShortLinkResponse;
import org.linkservice.service.LinkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@AllArgsConstructor
public class LinkController {
    private final LinkService linkService;

    @PostMapping("/shorten")
    public CreateShortLinkResponse createShortLink(@RequestBody CreateShortLinkRequest request) {
        return linkService.createShortLink(request);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode,
                                         @RequestHeader("userId") UUID userId,
                                         HttpServletResponse response) throws IOException {
        return linkService.getOriginalUrl(userId, shortCode)
                .map(originalUrl -> {
                    try {
                        response.sendRedirect(originalUrl);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return ResponseEntity.status(HttpStatus.FOUND).<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
