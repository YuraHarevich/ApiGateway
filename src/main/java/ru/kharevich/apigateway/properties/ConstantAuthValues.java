package ru.kharevich.apigateway.properties;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConstantAuthValues {
    public static final String AUTHORIZATION_PREFIX = "Bearer ";
    public static final String AUTHORIZATION_USER_ROLE_PREFIX = "X-User-Roles";
    public static final String AUTHORIZATION_USER_ID_PREFIX = "X-User-Id";
    public static final List<String> OPEN_API_ENDPOINTS = List.of(
            "api/v1/auth/sign-in",
            "api/v1/auth/sign-up",
            "api/v1/auth/refresh",
            "/api/v1/auth/validate"
    );;

}
