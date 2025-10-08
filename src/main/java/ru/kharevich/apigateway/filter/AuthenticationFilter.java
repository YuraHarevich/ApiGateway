package ru.kharevich.apigateway.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.kharevich.apigateway.dto.UserInfo;
import ru.kharevich.apigateway.dto.ValidateTokenRequest;
import ru.kharevich.apigateway.properties.ServiceProperties;

import java.util.List;

public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final ServiceProperties serviceProperties;

    private final List<String> openApiEndpoints = List.of(
            "api/v1/auth/sign-in",
            "api/v1/auth/sign-up",
            "api/v1/auth/refresh",
            "/api/v1/auth/validate"
    );

    private final WebClient.Builder webClientBuilder;

    public AuthenticationFilter(WebClient.Builder webClientBuilder, ServiceProperties serviceProperties) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
        this.serviceProperties = serviceProperties;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            if (openApiEndpoints.contains(path)) {
                return chain.filter(exchange);
            }
            System.out.println("---------Method----------:" + exchange.getRequest().getMethod());

            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }
            String token = authHeader.substring(7);

            return webClientBuilder.build()
                    .post()
                    .uri(serviceProperties.getValidateJwtPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new ValidateTokenRequest(token))
                    .retrieve()
                    .bodyToMono(UserInfo.class)
                    .flatMap(userInfo -> {
                        exchange.getRequest().mutate()
                                .header("X-User-Id", userInfo.id())
                                .header("X-User-Roles", String.join(",", userInfo.roles()));
                        return chain.filter(exchange);
                    })
                    .onErrorResume(e -> {
                        return onError(exchange, HttpStatus.UNAUTHORIZED);
                    });
        });
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();

    }

    public static class Config {

    }

}
