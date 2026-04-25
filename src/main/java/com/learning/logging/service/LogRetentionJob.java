package com.learning.logging.service;

import com.learning.logging.config.DbLoggingProperties;
import com.learning.logging.repo.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled job to clean up old database logs based on the retention policy.
 * Runs daily at 2 AM by default.
 */
public class LogRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(LogRetentionJob.class);

    private final LogRepository logRepository;
    private final DbLoggingProperties properties;

    public LogRetentionJob(LogRepository logRepository, DbLoggingProperties properties) {
        this.logRepository = logRepository;
        this.properties = properties;
    }

    @Scheduled(cron = "${db.logging.retention-cron:0 0 2 * * ?}")
    @Transactional
    public void cleanupOldLogs() {
        int retentionDays = properties.getRetentionDays();
        if (retentionDays <= 0) {
            log.info("Log retention policy disabled (retentionDays <= 0). Skipping cleanup.");
            return;
        }

        Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        log.info("Starting log cleanup. Deleting logs older than {} days (cutoff: {})", retentionDays, cutoffDate);

        try {
            logRepository.deleteOlderThan(cutoffDate);
            log.info("Successfully finished log cleanup.");
        } catch (Exception e) {
            log.error("Error occurred during log cleanup", e);
        }
    }
}
