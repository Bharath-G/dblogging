# Spring Boot DB Logging Starter

A lightweight Spring Boot starter library that automatically intercepts and logs HTTP requests, responses, and exceptions into a relational database. It's designed to be a drop-in dependency for microservices to maintain an audit trail of incoming traffic.

## Features

- **Automatic Interception:** Captures HTTP method, URI, request body, response body, response code, and execution time.
- **Asynchronous Persistence:** Uses Spring's `@Async` to persist logs without delaying the HTTP response to the client.
- **Sensitive Data Masking:** Automatically masks sensitive fields like passwords, tokens, and authorization headers in JSON bodies.
- **Payload Truncation:** Configurable maximum body size to prevent huge payloads from bloating the database.
- **Exception Logging:** Captures exception classes and messages if a request fails.
- **Distributed Tracing Support:** Generates and propagates `X-Correlation-ID` headers to trace requests across microservices.
- **Zero-Config Schema:** Bundled Flyway migration automatically creates the required `LOG` table on startup.
- **Automated Cleanup:** Scheduled background job automatically deletes logs older than the configured retention period.

## Getting Started

### 1. Add Dependency

Add the starter library to your Spring Boot project's `pom.xml` (after publishing it to your local or remote Maven repository):

```xml
<dependency>
    <groupId>com.learning</groupId>
    <artifactId>logging</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 2. Required Setup

Ensure your consumer application has Spring Data JPA and Flyway configured, as the starter relies on them. The application must also have a configured database connection.

```yaml
spring:
  application:
    name: my-microservice # Used to populate the SERVICE_NAME column
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: myuser
    password: mypassword
```

## Configuration

You can customize the behavior in your `application.yaml` or `application.properties`:

```yaml
db:
  logging:
    enabled: true             # Enable or disable the logging filter (default: true)
    log-request-body: true    # Log incoming request bodies (default: true)
    log-response-body: true   # Log outgoing response bodies (default: true)
    max-body-size: 10000      # Truncate payloads larger than this size in characters (default: 10000)
    retention-days: 30        # How many days to keep logs before deletion (default: 30)
    exclude-paths:            # Endpoints to ignore
      - /actuator
      - /swagger
      - /v3/api-docs
```

## Database Schema

The library uses Flyway to automatically generate the following schema (`LOG` table):

| Column | Type | Description |
|---|---|---|
| `ID` | VARCHAR(36) | Primary Key (matches Correlation-ID) |
| `METHOD` | VARCHAR(30) | HTTP Method (GET, POST, etc.) |
| `REQUEST_BODY` | TEXT | Truncated & masked request payload |
| `RESPONSE_BODY` | TEXT | Truncated & masked response payload OR Exception details |
| `TYPE` | VARCHAR(20) | `REQUEST`, `RESPONSE`, or `EXCEPTION` |
| `RESPONSE_CODE` | INTEGER | HTTP Status Code |
| `DURATION_MS` | BIGINT | Request execution time in milliseconds |
| `SERVICE_NAME` | VARCHAR(100) | Name of the microservice serving the request |
| `REQUEST_URI` | VARCHAR(500) | Path of the HTTP request |
| `CREATED_AT` | TIMESTAMP | Auto-generated timestamp |

## Extending & Customizing

Because the auto-configuration uses `@ConditionalOnMissingBean`, you can provide your own bean definitions in your consumer application to override the default behavior. For instance, you can define your own `DbLoggingFilter` or `LogRetentionJob` bean, and the starter will back off and use yours.
