# Scholar Backend

Enterprise-grade CV matching and outreach platform built with Spring Boot 3.x, Java 17, and PostgreSQL.

## Features

- **Multi-tenant Architecture**: Complete data isolation with tenant-based access control
- **CV Management**: Secure upload, parsing, and text extraction from PDF/DOCX files
- **Intelligent Matching**: Keyword-based matching engine with weighted scoring algorithm
- **Email Campaigns**: Batch processing with rate limiting and retry logic
- **Security**: AES-GCM encryption for sensitive data, input validation
- **Scalability**: Async processing, connection pooling, database indexing
- **API Documentation**: Comprehensive Swagger/OpenAPI 3.0 documentation

## Architecture

### Technology Stack

- **Java**: 17
- **Spring Boot**: 3.2.5
- **Database**: PostgreSQL with Flyway migrations
- **Security**: Spring Security with AES-GCM encryption
- **Documentation**: SpringDoc OpenAPI 3
- **Document Processing**: Apache PDFBox, Apache POI

### Package Structure

```
com.scholar/
├── config/           # Application configuration
├── controller/       # REST API controllers
├── domain/
│   ├── entity/      # JPA entities
│   └── repository/  # Spring Data repositories
├── dto/
│   ├── request/     # Request DTOs
│   └── response/    # Response DTOs
├── exception/       # Exception handlers
└── service/
    ├── cv/          # CV management services
    ├── email/       # Email campaign services
    ├── matching/    # Matching engine
    ├── security/    # Encryption services
    └── storage/     # File storage services
```

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- (Optional) Docker for PostgreSQL

## Quick Start

### 1. Database Setup

Create PostgreSQL database:

```bash
createdb scholar_db
```

Or using Docker:

```bash
docker run -d \
  --name scholar-postgres \
  -e POSTGRES_DB=scholar_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:14
```

### 2. Configuration

Set environment variables (or update `application-dev.properties`):

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export ENCRYPTION_KEY="your-32-character-secret-key!"
export CV_STORAGE_PATH=./storage/cvs
```

### 3. Build and Run

```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The application will start on `http://localhost:9090/api`

### 4. Access Swagger UI

Open browser to: `http://localhost:9090/api/swagger-ui.html`

## API Endpoints

### CV Management

- `POST /v1/cvs/upload` - Upload CV file
- `GET /v1/cvs` - List all CVs
- `GET /v1/cvs/{cvId}` - Get CV details
- `POST /v1/cvs/{cvId}/compute-matches` - Compute professor matches
- `DELETE /v1/cvs/{cvId}` - Delete CV

### Match Results

- `GET /v1/matches/cv/{cvId}` - Get all matches for CV
- `GET /v1/matches/cv/{cvId}/above-threshold` - Get matches above score threshold
- `POST /v1/matches/cv/{cvId}/recompute` - Recompute matches

### Email Campaigns

- `POST /v1/campaigns` - Create email campaign
- `GET /v1/campaigns` - List all campaigns
- `GET /v1/campaigns/{campaignId}` - Get campaign details
- `POST /v1/campaigns/{campaignId}/schedule` - Schedule campaign
- `POST /v1/campaigns/{campaignId}/execute` - Execute campaign
- `POST /v1/campaigns/{campaignId}/cancel` - Cancel campaign
- `GET /v1/campaigns/{campaignId}/logs` - Get email logs

### SMTP Account

- `POST /v1/smtp` - Configure SMTP account
- `GET /v1/smtp` - Get SMTP account
- `POST /v1/smtp/deactivate` - Deactivate SMTP account

## Database Schema

### Core Tables

- `tenant` - Multi-tenant organizations
- `user_profile` - User profiles within tenants
- `cv` - Uploaded CVs with parsing status
- `cv_keyword` - Extracted keywords from CVs
- `university` - Academic institutions
- `professor` - Academic researchers
- `professor_keyword` - Research keywords
- `match_result` - CV-Professor matches
- `smtp_account` - Email sending configuration
- `email_campaign` - Email outreach campaigns
- `email_log` - Individual email send logs
- `email_blacklist` - Blocked email addresses

## Matching Algorithm

The matching engine uses a deterministic weighted scoring algorithm:

1. **Keyword Extraction**: Extracts keywords from CV text with TF-based weighting
2. **Normalization**: Converts to lowercase, removes stop words
3. **Intersection**: Finds common keywords between CV and professor
4. **Weighted Scoring**: Computes score based on keyword weights
5. **Normalization**: Normalizes score to [0, 1] range

**Formula**: 
```
match_score = Σ(cv_weight[k] × prof_weight[k]) / Σ(cv_weight[all])
```

## Email Campaigns

### Features

- **Batch Processing**: Processes emails in configurable batches
- **Rate Limiting**: Respects Gmail rate limits (30 emails/minute default)
- **Retry Logic**: Exponential backoff with configurable retry attempts
- **Blacklist Support**: Prevents emails to blacklisted addresses
- **Idempotency**: Prevents duplicate emails to same professor per campaign
- **Template Support**: Simple variable substitution in email templates

### Template Variables

- `{{professor_name}}` - Full name
- `{{university}}` - University name
- `{{match_score}}` - Match score
- `{{matched_keywords}}` - Comma-separated matched keywords

## Security

### Encryption

- **Algorithm**: AES-GCM-256
- **Usage**: SMTP passwords encrypted at rest
- **Key Management**: Environment variable configuration

### Input Validation

- File type validation (PDF, DOCX only)
- File size limits (10MB default)
- Request parameter validation
- SQL injection prevention (JPA parameterized queries)

### Multi-tenant Isolation

- Tenant ID required for all data access
- Repository-level tenant filtering
- Foreign key constraints enforce data integrity

## Performance Optimizations

- **Async Processing**: CV parsing and matching run asynchronously
- **Connection Pooling**: HikariCP with optimized settings
- **Database Indexing**: Comprehensive indexes on frequently queried columns
- **Lazy Loading**: JPA entities use lazy loading by default
- **Batch Operations**: Email sending uses batching
- **N+1 Query Prevention**: Explicit fetch strategies

## Configuration Properties

Key configuration properties (see `application.properties`):

```properties
# File Upload
scholar.cv.storage.path=./storage/cvs
scholar.cv.allowed-types=application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document
scholar.cv.max-size-mb=10

# Email
scholar.email.batch-size=50
scholar.email.rate-limit-per-minute=30
scholar.email.retry-attempts=3
scholar.email.retry-delay-ms=5000

# Security
scholar.security.encryption.key=${ENCRYPTION_KEY}
```

## Production Deployment

### Prerequisites

1. Set environment variables:
   - `DATABASE_URL` - PostgreSQL connection URL
   - `DB_USERNAME` - Database username
   - `DB_PASSWORD` - Database password
   - `ENCRYPTION_KEY` - 32-character encryption key
   - `CV_STORAGE_PATH` - File storage path

2. Configure profile:
   ```bash
   export SPRING_PROFILES_ACTIVE=prod
   ```

3. Build production JAR:
   ```bash
   mvn clean package -DskipTests
   ```

4. Run:
   ```bash
   java -jar target/scholar-backend-1.0.0.jar
   ```

### Docker Deployment

```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/scholar-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Development

### Running Tests

```bash
mvn test
```

### Code Quality

The codebase follows enterprise best practices:

- Lombok for boilerplate reduction
- SLF4J for structured logging
- Bean Validation for input validation
- Transaction management
- Exception handling
- Comprehensive documentation

## License

Proprietary - All rights reserved

## Support

For support, contact: support@scholar.com
