package org.linkservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ShortLink {
    private String longUrl;
    private Instant createdAt;
    private int remainingClicks;
}
