package ru.kharevich.apigateway.dto;

import java.util.List;

public record UserInfo(
        String id,
        List<String> roles
) {
}
