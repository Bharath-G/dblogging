package com.learning.logging.filter;

import com.learning.logging.config.DbLoggingProperties;
import com.learning.logging.service.LogPersistenceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that intercepts every HTTP request/response and delegates
 * persistence to {@link LogPersistenceService} — a separate Spring-managed bean.
 * The async save MUST go through an injected dependency (not 'this') so that
 * Spring's AOP proxy is respected and @Async actually runs on a thread-pool thread.
 */
public class DbLoggingFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_CORRELATION_ID = "Correlation-ID";

    private final LogPersistenceService persistenceService;
    private final DbLoggingProperties properties;
    private final String serviceName;

    public DbLoggingFilter(LogPersistenceService persistenceService,
                           DbLoggingProperties properties,
                           @Value("${spring.application.name:unknown}") String serviceName) {
        this.persistenceService = persistenceService;
        this.properties = properties;
        this.serviceName = serviceName;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return properties.getExcludePaths().stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get or generate correlation ID
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_CORRELATION_ID, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        long startTime = System.currentTimeMillis();
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 1);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        Throwable caughtException = null;
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } catch (Exception ex) {
            caughtException = ex;
            throw ex; // re-throw so Spring still handles the error response
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Delegate to injected bean — NOT 'this' — so @Async proxy is honoured
            persistenceService.persist(
                    wrappedRequest, wrappedResponse,
                    duration, UUID.fromString(correlationId),
                    serviceName, caughtException
            );

            wrappedResponse.copyBodyToResponse();
            MDC.remove(MDC_CORRELATION_ID);
        }
    }
}