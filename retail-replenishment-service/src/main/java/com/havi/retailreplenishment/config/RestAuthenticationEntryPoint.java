package com.havi.retailreplenishment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.havi.retailreplenishment.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Missing/invalid bearer token. Spring Security's filter chain runs before
 * the DispatcherServlet, so GlobalExceptionHandler never sees this case —
 * without this bean the response body would be Spring's default text/html
 * error page instead of the same ErrorResponse shape every other endpoint
 * returns.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        ErrorResponse body = new ErrorResponse(
            LocalDateTime.now(), HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
            "A valid bearer token is required for this request.", request.getRequestURI());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
