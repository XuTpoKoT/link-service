package org.linkservice.controller.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateShortLinkRequest {
    private UUID uuid;
    private String longUrl;
}
