package com.learning.logging.service;

import com.learning.logging.config.DbLoggingProperties;
import com.learning.logging.entity.Log;
import com.learning.logging.repo.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Separate Spring-managed bean that owns the async DB-save logic.
 *
 * Why a separate bean?
 * Spring's @Async works through a proxy. Calling an @Async method on 'this'
 * (self-invocation) bypasses the proxy entirely, making the call synchronous.
 * By extracting the logic here and injecting this service into DbLoggingFilter,
 * the call goes through the Spring proxy and is truly async.
 */
@Service
public class LogPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(LogPersistenceService.class);

    // Masks values of sensitive JSON fields (password, token, secret, authorization, cvv, ssn)
    private static final Pattern MASK_PATTERN = Pattern.compile(
            "(?i)(\"(password|token|secret|authorization|cvv|ssn)\"\\s*:\\s*\")([^\"]*)(\")",
            Pattern.CASE_INSENSITIVE
    );

    private final LogRepository repository;
    private final DbLoggingProperties properties;

    public LogPersistenceService(LogRepository repository, DbLoggingProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * Persists the HTTP log entry asynchronously via Spring's thread-pool executor.
     * Called from DbLoggingFilter through this injected bean so the AOP proxy is respected.
     */
    @Async
    public void persist(ContentCachingRequestWrapper request,
                        ContentCachingResponseWrapper response,
                        long duration,
                        UUID correlationId,
                        String serviceName,
                        Throwable exception) {
        Log entry = new Log();
        try {
            entry.setId(correlationId);
            entry.setMethod(request.getMethod());
            entry.setDurationMs(duration);
            entry.setResponseCode(response.getStatus());
            entry.setServiceName(serviceName);
            entry.setRequestUri(request.getRequestURI());

            if (exception != null) {
                // Exception details stored in response_body; TYPE = EXCEPTION identifies the row
                String exceptionBody = "exception_class: " + exception.getClass().getName()
                        + "\nexception_message: " + exception.getMessage();
                entry.setResponseBody(exceptionBody);
                entry.setType("EXCEPTION");
            } else {
                if (properties.isLogRequestBody()) {
                    String raw = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
                    entry.setRequestBody(truncateAndMask(raw));
                    entry.setType("REQUEST");
                }
                if (properties.isLogResponseBody()) {
                    String raw = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
                    entry.setResponseBody(truncateAndMask(raw));
                    entry.setType("RESPONSE");
                }
            }

            repository.save(entry);
        } catch (Exception e) {
            log.error("Failed to save request log for correlationId: {}", correlationId, e);
        }
    }

    /**
     * Truncates body to maxBodySize and masks sensitive field values.
     */
    private String truncateAndMask(String body) {
        if (!StringUtils.hasText(body)) return body;
        String truncated = body.length() > properties.getMaxBodySize()
                ? body.substring(0, properties.getMaxBodySize()) + "...[TRUNCATED]"
                : body;
        return MASK_PATTERN.matcher(truncated).replaceAll("$1****$4");
    }
}
