package com.nextgen.bank.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class IdempotencyKeyInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyKeyInterceptor.class);
    public static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    private final IdempotencyStore idempotencyStore;

    public IdempotencyKeyInterceptor(@Autowired(required = false) IdempotencyStore idempotencyStore) {
        this.idempotencyStore = idempotencyStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Only enforce on mutating HTTP methods (POST, PUT, PATCH)
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method) && !"PATCH".equalsIgnoreCase(method)) {
            return true;
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            // If header is absent on non-critical endpoints, allow request or log warning
            return true;
        }

        if (idempotencyStore != null && idempotencyStore.exists(idempotencyKey)) {
            log.info("Idempotent request detected with key [{}]", idempotencyKey);
            Optional<String> cachedResponse = idempotencyStore.getCachedResponse(idempotencyKey);
            if (cachedResponse.isPresent()) {
                response.setContentType("application/json");
                response.setStatus(HttpStatus.OK.value());
                response.getWriter().write(cachedResponse.get());
                return false; // Stop further handler execution
            }
        }

        request.setAttribute(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
        return true;
    }
}
