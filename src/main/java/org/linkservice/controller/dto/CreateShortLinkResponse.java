package org.linkservice.controller.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateShortLinkResponse {
    private UUID uuid;
    private String shortUrl;
}
