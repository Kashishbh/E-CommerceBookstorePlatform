package com.booknest.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();
        String query = exchange.getRequest().getURI().getQuery();
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        log.info("Incoming Request -> Method: {}, Path: {}, Query: {}, Authorization Present: {}",
                method, path, query, authHeader != null);

        long startTime = System.currentTimeMillis();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long timeTaken = System.currentTimeMillis() - startTime;

            if (exchange.getResponse().getStatusCode() != null) {
                log.info("Outgoing Response -> Path: {}, Status: {}, Time Taken: {} ms",
                        path,
                        exchange.getResponse().getStatusCode().value(),
                        timeTaken);
            } else {
                log.info("Outgoing Response -> Path: {}, Status: N/A, Time Taken: {} ms",
                        path,
                        timeTaken);
            }
        }));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
