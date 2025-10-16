package ru.kharevich.apigateway.filter;

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

import static ru.kharevich.apigateway.properties.ConstantAuthValues.AUTHORIZATION_PREFIX;
import static ru.kharevich.apigateway.properties.ConstantAuthValues.AUTHORIZATION_USER_ID_PREFIX;
import static ru.kharevich.apigateway.properties.ConstantAuthValues.AUTHORIZATION_USER_ROLE_PREFIX;
import static ru.kharevich.apigateway.properties.ConstantAuthValues.OPEN_API_ENDPOINTS;

public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final ServiceProperties serviceProperties;

    private final List<String> openApiEndpoints = OPEN_API_ENDPOINTS;

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
            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith(AUTHORIZATION_PREFIX)) {
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
                                .header(AUTHORIZATION_USER_ID_PREFIX, userInfo.id())
                                .header(AUTHORIZATION_USER_ROLE_PREFIX, String.join(",", userInfo.roles()));
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
