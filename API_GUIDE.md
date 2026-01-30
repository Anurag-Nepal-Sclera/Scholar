# API Usage Guide

Complete guide for using the Scholar Backend API.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Authentication](#authentication)
3. [CV Upload Flow](#cv-upload-flow)
4. [Matching Flow](#matching-flow)
5. [Email Campaign Flow](#email-campaign-flow)
6. [Error Handling](#error-handling)

## Getting Started

Base URL: `http://localhost:9090/api`

All responses follow this format:

```json
{
  "success": true|false,
  "message": "Optional message",
  "data": {...},
  "error": null
}
```

## Authentication

**Note**: Current implementation accepts `tenantId` as a request parameter for simplicity. In production, implement proper JWT/OAuth authentication where `tenantId` is extracted from the authenticated user's session.

## CV Upload Flow

### Step 1: Upload CV

```bash
curl -X POST "http://localhost:9090/api/v1/cvs/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/cv.pdf" \
  -F "tenantId=880e8400-e29b-41d4-a716-446655440001"
```

**AI-Powered Parsing**: The system now uses DeepSeek-R1 (via OpenRouter) to extract the top 15 research keywords most relevant for academic outreach.

### Step 1.5: Manually Trigger Parsing (Optional)

If parsing failed or you want to re-run it:

```bash
curl -X POST "http://localhost:9090/api/v1/cvs/{cvId}/parse?tenantId=880e8400-e29b-41d4-a716-446655440001"
```

### Step 2: Check CV Status

```bash
curl "http://localhost:9090/api/v1/cvs/{cvId}?tenantId=880e8400-e29b-41d4-a716-446655440001"
```

**Response (when completed):**
```json
{
  "success": true,
  "data": {
    "id": "aa0e8400-e29b-41d4-a716-446655440001",
    "originalFilename": "cv.pdf",
    "parsingStatus": "COMPLETED",
    "parsedAt": "2026-01-30T10:31:00",
    "keywordCount": 47
  }
}
```

### Step 3: List All CVs

```bash
curl "http://localhost:9090/api/v1/cvs?tenantId=880e8400-e29b-41d4-a716-446655440001&page=0&size=20"
```

## Matching Flow

### Step 1: Compute Matches

**Prerequisite**: CV must have `parsingStatus: COMPLETED`.

```bash
curl -X POST "http://localhost:9090/api/v1/cvs/{cvId}/compute-matches?tenantId=880e8400-e29b-41d4-a716-446655440001"
```

**Response:**
```json
{
  "success": true,
  "message": "Match computation started"
}
```

**Note**: Matching runs asynchronously. Wait 5-10 seconds depending on database size.

### Step 2: Retrieve Matches

Get all matches ordered by score:

```bash
curl "http://localhost:9090/api/v1/matches/cv/{cvId}?tenantId=880e8400-e29b-41d4-a716-446655440001&page=0&size=20"
```

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "bb0e8400-e29b-41d4-a716-446655440001",
        "professor": {
          "id": "660e8400-e29b-41d4-a716-446655440001",
          "firstName": "John",
          "lastName": "Doe",
          "email": "john.doe@mit.edu",
          "department": "Computer Science",
          "universityName": "Massachusetts Institute of Technology",
          "universityCountry": "USA"
        },
        "matchScore": 0.852000,
        "matchedKeywords": "machine learning, deep learning, neural networks",
        "totalCvKeywords": 47,
        "totalProfessorKeywords": 5,
        "totalMatchedKeywords": 3
      }
    ],
    "totalElements": 10,
    "totalPages": 1
  }
}
```

### Step 3: Get High-Quality Matches Only

Filter by minimum score (e.g., 0.5 = 50% match):

```bash
curl "http://localhost:9090/api/v1/matches/cv/{cvId}/above-threshold?tenantId=880e8400-e29b-41d4-a716-446655440001&minScore=0.5"
```

### Step 4: Recompute Matches

If professor keywords are updated, recompute:

```bash
curl -X POST "http://localhost:9090/api/v1/matches/cv/{cvId}/recompute?tenantId=880e8400-e29b-41d4-a716-446655440001"
```

## Email Campaign Flow

### Step 1: Configure SMTP Account

**Gmail Configuration:**

```bash
curl -X POST "http://localhost:9090/api/v1/smtp?tenantId=880e8400-e29b-41d4-a716-446655440001" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your-email@gmail.com",
    "smtpHost": "smtp.gmail.com",
    "smtpPort": 587,
    "username": "your-email@gmail.com",
    "password": "your-app-specific-password",
    "useTls": true,
    "useSsl": false,
    "fromName": "Student Name"
  }'
```

**Important**: For Gmail, use an [App Password](https://support.google.com/accounts/answer/185833), not your regular password.

### Step 2: Create Campaign

```bash
curl -X POST "http://localhost:9090/api/v1/campaigns?tenantId=880e8400-e29b-41d4-a716-446655440001" \
  -H "Content-Type: application/json" \
  -d '{
    "cvId": "aa0e8400-e29b-41d4-a716-446655440001",
    "name": "PhD Application Outreach 2026",
    "subject": "Prospective PhD Student - Research Interests",
    "bodyTemplate": "Dear Prof. {{professor_name}},\n\nI am writing to express my interest in pursuing a PhD at {{university}}.\n\nOur research interests align particularly well ({{match_score}} match) in areas such as {{matched_keywords}}.\n\nBest regards,\nStudent Name",
    "minMatchScore": 0.6
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Campaign created successfully",
  "data": {
    "id": "cc0e8400-e29b-41d4-a716-446655440001",
    "name": "PhD Application Outreach 2026",
    "status": "DRAFT",
    "totalRecipients": 8,
    "sentCount": 0,
    "failedCount": 0
  }
}
```

**Template Variables:**
- `{{professor_name}}` - Full name
- `{{university}}` - University name
- `{{match_score}}` - Match score (e.g., 0.852000)
- `{{matched_keywords}}` - Comma-separated matched keywords

### Step 3: Schedule Campaign

Schedule for future execution:

```bash
curl -X POST "http://localhost:9090/api/v1/campaigns/{campaignId}/schedule?tenantId=880e8400-e29b-41d4-a716-446655440001&scheduledAt=2026-02-01T09:00:00"
```

Or execute immediately:

```bash
curl -X POST "http://localhost:9090/api/v1/campaigns/{campaignId}/execute"
```

**AI-Generated Outreach**: When a campaign is executed, the system uses AI to generate a highly personalized research outreach email for each professor, matching the student's research interests with the professor's expertise.

### Step 4: Monitor Campaign

```bash
curl "http://localhost:9090/api/v1/campaigns/{campaignId}?tenantId=880e8400-e29b-41d4-a716-446655440001"
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "cc0e8400-e29b-41d4-a716-446655440001",
    "name": "PhD Application Outreach 2026",
    "status": "COMPLETED",
    "totalRecipients": 8,
    "sentCount": 7,
    "failedCount": 1,
    "startedAt": "2026-01-30T14:00:00",
    "completedAt": "2026-01-30T14:05:23"
  }
}
```

### Step 5: View Email Logs

```bash
curl "http://localhost:9090/api/v1/campaigns/{campaignId}/logs?page=0&size=20"
```

**Response includes:**
- Recipient email
- Sent status
- Timestamp
- Error messages (if failed)

### Step 6: Cancel Campaign

If campaign is scheduled but not yet started:

```bash
curl -X POST "http://localhost:9090/api/v1/campaigns/{campaignId}/cancel?tenantId=880e8400-e29b-41d4-a716-446655440001"
```

## Complete Workflow Example

### Scenario: Upload CV and Send Emails to Top Matches

```bash
# 1. Upload CV
CV_RESPONSE=$(curl -X POST "http://localhost:9090/api/v1/cvs/upload" \
  -F "file=@cv.pdf" \
  -F "tenantId=880e8400-e29b-41d4-a716-446655440001" \
  -F "userProfileId=990e8400-e29b-41d4-a716-446655440001")

CV_ID=$(echo $CV_RESPONSE | jq -r '.data.id')

# 2. Wait for parsing (check status)
sleep 10

# 3. Compute matches
curl -X POST "http://localhost:9090/api/v1/cvs/${CV_ID}/compute-matches?tenantId=880e8400-e29b-41d4-a716-446655440001"

# 4. Wait for matching
sleep 10

# 5. Check top matches
curl "http://localhost:9090/api/v1/matches/cv/${CV_ID}/above-threshold?tenantId=880e8400-e29b-41d4-a716-446655440001&minScore=0.6"

# 6. Create campaign
curl -X POST "http://localhost:9090/api/v1/campaigns?tenantId=880e8400-e29b-41d4-a716-446655440001" \
  -H "Content-Type: application/json" \
  -d "{
    \"cvId\": \"${CV_ID}\",
    \"name\": \"Outreach Campaign\",
    \"subject\": \"PhD Application\",
    \"bodyTemplate\": \"Dear Prof. {{professor_name}},...\",
    \"minMatchScore\": 0.6
  }"
```

## Error Handling

### Common Error Responses

**400 Bad Request - Validation Error:**
```json
{
  "success": false,
  "message": "Validation failed",
  "error": {
    "email": "Invalid email format",
    "minMatchScore": "Score must be between 0 and 1"
  }
}
```

**400 Bad Request - Business Logic Error:**
```json
{
  "success": false,
  "message": "CV not found or access denied"
}
```

**413 Payload Too Large:**
```json
{
  "success": false,
  "message": "File size exceeds maximum allowed limit"
}
```

**500 Internal Server Error:**
```json
{
  "success": false,
  "message": "An unexpected error occurred. Please try again later."
}
```

## Rate Limits

### Email Sending

- **Default**: 30 emails per minute (configurable)
- **Gmail**: Recommend 20 emails/minute for app passwords
- **Batch Size**: 50 emails per batch

Configure in `application.properties`:
```properties
scholar.email.rate-limit-per-minute=30
scholar.email.batch-size=50
```

## Best Practices

### 1. CV Upload
- Use PDF or DOCX format only
- Maximum file size: 10MB
- Include rich text content for better keyword extraction

### 2. Matching
- Allow at least 5-10 seconds for matching to complete
- Use `minMatchScore` of 0.5-0.7 for quality matches
- Recompute matches if professor data is updated

### 3. Email Campaigns
- Test email template with a small campaign first
- Use personalization variables for better engagement
- Check spam folder if emails not received
- Monitor campaign logs for failures

### 4. SMTP Configuration
- **Gmail**: Enable 2FA and use App Passwords
- **Office 365**: Use modern authentication
- Store SMTP credentials securely (already encrypted by system)

## Pagination

All list endpoints support pagination:

**Query Parameters:**
- `page`: Page number (0-indexed, default: 0)
- `size`: Page size (default: 20, max: 100)
- `sort`: Sort field and direction (e.g., `createdAt,desc`)

**Example:**
```bash
curl "http://localhost:9090/api/v1/cvs?tenantId=xxx&page=0&size=10&sort=uploadedAt,desc"
```

## Swagger Documentation

Interactive API documentation available at:
```
http://localhost:9090/api/swagger-ui.html
```

Test all endpoints directly from the browser with Swagger UI.
