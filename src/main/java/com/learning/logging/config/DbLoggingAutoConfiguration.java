package com.learning.logging.config;

import com.learning.logging.entity.Log;
import com.learning.logging.filter.DbLoggingFilter;
import com.learning.logging.repo.LogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@EnableConfigurationProperties(DbLoggingProperties.class)
@EntityScan(basePackageClasses = Log.class)
@EnableJpaRepositories(basePackageClasses = LogRepository.class)
@ConditionalOnProperty(prefix = "db.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableAsync  // Required for @Async on persistLog
@EnableScheduling // Required for scheduled jobs like LogRetentionJob
public class DbLoggingAutoConfiguration {

    /**
     * Define the persistence service bean explicitly to ensure it is created and proxied
     * by the auto-configuration when used as a starter.
     */
    @Bean
    @ConditionalOnMissingBean
    public com.learning.logging.service.LogPersistenceService logPersistenceService(LogRepository repository, DbLoggingProperties properties) {
        return new com.learning.logging.service.LogPersistenceService(repository, properties);
    }

    /**
     * #5 – @ConditionalOnMissingBean: lets consumer apps supply their own filter bean
     * without getting a duplicate-bean conflict.
     */
    @Bean
    @ConditionalOnMissingBean
    public DbLoggingFilter dbLoggingFilter(com.learning.logging.service.LogPersistenceService persistenceService,
                                           DbLoggingProperties properties,
                                           @Value("${spring.application.name:unknown}") String serviceName) {
        return new DbLoggingFilter(persistenceService, properties, serviceName);
    }

    /**
     * #6 – FilterRegistrationBean: explicit Servlet filter registration with a well-defined
     * order (Ordered.HIGHEST_PRECEDENCE + 10) so the filter runs as early as possible,
     * before security or other filters consume the request body.
     */
    @Bean
    @ConditionalOnMissingBean(name = "dbLoggingFilterRegistration")
    public FilterRegistrationBean<DbLoggingFilter> dbLoggingFilterRegistration(
            DbLoggingFilter dbLoggingFilter) {
        FilterRegistrationBean<DbLoggingFilter> registration = new FilterRegistrationBean<>(dbLoggingFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("dbLoggingFilter");
        return registration;
    }

    /**
     * Register the scheduled log retention job.
     */
    @Bean
    @ConditionalOnMissingBean
    public com.learning.logging.service.LogRetentionJob logRetentionJob(LogRepository logRepository, DbLoggingProperties properties) {
        return new com.learning.logging.service.LogRetentionJob(logRepository, properties);
    }
}