package com.learning.logging.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="LOG")
@Data
public class Log {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name="ID", length = 36, nullable = false)
    private UUID id;

    @Column(name="METHOD", length = 30)
    private String method;

    @Lob
    @Column(name="REQUEST_BODY")
    private String requestBody;

    @Lob
    @Column(name = "RESPONSE_BODY")
    private String responseBody;

    @Column(name="TYPE", length = 20)
    private String type;

    @Column(name = "CREATED_AT")
    private Instant createdAt = Instant.now();

    @Column(name = "RESPONSE_CODE")
    private Integer responseCode;

    @Column(name = "DURATION_MS")
    private Long durationMs;

    @Column(name = "SERVICE_NAME", length = 50)
    private String serviceName;

    @Column(name = "REQUEST_URI", length = 500)
    private String requestUri;

}
