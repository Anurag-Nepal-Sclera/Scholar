# Production Deployment Checklist

## Pre-Deployment

### Database Setup
- [ ] Create PostgreSQL database (version 14+)
- [ ] Configure database user with appropriate permissions
- [ ] Set up database backups
- [ ] Configure connection pooling limits
- [ ] Test database connectivity

### Environment Configuration
- [ ] Set `DATABASE_URL` environment variable
- [ ] Set `DB_USERNAME` environment variable
- [ ] Set `DB_PASSWORD` environment variable
- [ ] Generate secure 32-character `ENCRYPTION_KEY`
- [ ] Set `CV_STORAGE_PATH` for file storage
- [ ] Set `SPRING_PROFILES_ACTIVE=prod`

### Security
- [ ] Change default encryption key
- [ ] Configure firewall rules
- [ ] Set up SSL/TLS certificates
- [ ] Enable HTTPS for all endpoints
- [ ] Configure CORS policies
- [ ] Set up rate limiting (optional)
- [ ] Configure security headers

### Email Configuration
- [ ] Verify SMTP server settings
- [ ] Test email sending from application
- [ ] Configure email rate limits
- [ ] Set up email monitoring
- [ ] Add SPF/DKIM records (optional)

## Build Process

### Maven Build
```bash
# Clean and build
mvn clean package -DskipTests

# Verify JAR was created
ls -lh target/scholar-backend-1.0.0.jar
```

### Docker Build (Alternative)
```bash
# Build image
docker build -t scholar-backend:1.0.0 .

# Tag for registry
docker tag scholar-backend:1.0.0 your-registry/scholar-backend:1.0.0

# Push to registry
docker push your-registry/scholar-backend:1.0.0
```

## Deployment Steps

### Option 1: Direct JAR Deployment

1. **Copy JAR to server**
```bash
scp target/scholar-backend-1.0.0.jar user@server:/opt/scholar/
```

2. **Create systemd service** (`/etc/systemd/system/scholar.service`)
```ini
[Unit]
Description=Scholar Backend Service
After=postgresql.service

[Service]
Type=simple
User=scholar
WorkingDirectory=/opt/scholar
ExecStart=/usr/bin/java -jar /opt/scholar/scholar-backend-1.0.0.jar
Restart=always
RestartSec=10

Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="DATABASE_URL=jdbc:postgresql://localhost:5432/scholar_db"
Environment="DB_USERNAME=scholar_user"
Environment="DB_PASSWORD=secure_password"
Environment="ENCRYPTION_KEY=your-32-character-secret-key!"
Environment="CV_STORAGE_PATH=./storage/cvs"

[Install]
WantedBy=multi-user.target
```

3. **Start service**
```bash
sudo systemctl daemon-reload
sudo systemctl enable scholar
sudo systemctl start scholar
sudo systemctl status scholar
```

### Option 2: Docker Deployment

1. **Create production docker-compose.yml**
```yaml
version: '3.8'

services:
  scholar-backend:
    image: your-registry/scholar-backend:1.0.0
    restart: unless-stopped
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DATABASE_URL: ${DATABASE_URL}
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      ENCRYPTION_KEY: ${ENCRYPTION_KEY}
      CV_STORAGE_PATH: ./storage/cvs
    ports:
      - "9090:9090"
    volumes:
      - cv_storage:./storage/cvs
    depends_on:
      - postgres

  postgres:
    image: postgres:14
    restart: unless-stopped
    environment:
      POSTGRES_DB: scholar_db
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

volumes:
  cv_storage:
  postgres_data:
```

2. **Deploy**
```bash
docker-compose up -d
docker-compose logs -f scholar-backend
```

### Option 3: Kubernetes Deployment

See `k8s/` directory for Kubernetes manifests (not included in this basic setup).

## Post-Deployment

### Verification
- [ ] Check application logs
```bash
# SystemD
sudo journalctl -u scholar -f

# Docker
docker-compose logs -f scholar-backend
```

- [ ] Test health endpoint
```bash
curl http://localhost:9090/api/actuator/health
```

- [ ] Verify database migrations
```bash
# Connect to database
psql -U scholar_user -d scholar_db

# Check flyway_schema_history
SELECT * FROM flyway_schema_history;
```

- [ ] Test API endpoints
```bash
# Get Swagger UI
curl http://localhost:9090/api/swagger-ui.html
```

### Smoke Tests
- [ ] Upload a test CV
- [ ] Verify parsing completes
- [ ] Compute matches
- [ ] Configure SMTP account
- [ ] Create test campaign (don't execute)

### Monitoring Setup
- [ ] Configure application metrics
- [ ] Set up log aggregation
- [ ] Configure alerting rules
- [ ] Set up uptime monitoring
- [ ] Configure error tracking

## Security Hardening

### Application
- [ ] Disable Swagger in production (optional)
- [ ] Enable HTTPS only
- [ ] Configure session timeout
- [ ] Set up authentication (JWT/OAuth)
- [ ] Enable request logging
- [ ] Configure CORS properly

### Database
- [ ] Use strong passwords
- [ ] Enable SSL connections
- [ ] Restrict network access
- [ ] Regular backups
- [ ] Monitor connections

### Server
- [ ] Keep OS updated
- [ ] Configure firewall
- [ ] Disable unnecessary services
- [ ] Set up fail2ban
- [ ] Regular security audits

## Performance Tuning

### JVM Options
```bash
java -XX:+UseG1GC \
     -XX:MaxRAMPercentage=75.0 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/scholar \
     -jar scholar-backend-1.0.0.jar
```

### Database
- [ ] Analyze and optimize slow queries
- [ ] Configure connection pool size
- [ ] Set up read replicas (if needed)
- [ ] Regular VACUUM and ANALYZE
- [ ] Monitor index usage

### Application
- [ ] Tune async thread pool
- [ ] Configure email batch size
- [ ] Adjust rate limits
- [ ] Monitor memory usage
- [ ] Profile slow operations

## Backup Strategy

### Database Backups
```bash
# Daily backup script
pg_dump -U scholar_user scholar_db > backup_$(date +%Y%m%d).sql

# Restore
psql -U scholar_user scholar_db < backup_20260130.sql
```

### File Storage Backups
```bash
# Backup CVs
tar -czf cvs_backup_$(date +%Y%m%d).tar.gz ./storage/cvs

# Restore
tar -xzf cvs_backup_20260130.tar.gz -C ./storage/
```

## Rollback Plan

### Application Rollback
1. Stop current version
2. Deploy previous version JAR
3. Restart service
4. Verify functionality

### Database Rollback
1. Stop application
2. Restore database from backup
3. Rollback Flyway migrations (if needed)
4. Start application

## Maintenance

### Regular Tasks
- [ ] Weekly: Review application logs
- [ ] Weekly: Check disk space
- [ ] Monthly: Review security updates
- [ ] Monthly: Analyze performance metrics
- [ ] Quarterly: Review and update dependencies
- [ ] Quarterly: Security audit

### Log Rotation
Configure logrotate for application logs:
```
/var/log/scholar/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
}
```

## Troubleshooting

### Common Issues

**Application won't start**
- Check database connectivity
- Verify environment variables
- Check port availability
- Review application logs

**Database connection errors**
- Verify credentials
- Check network connectivity
- Confirm PostgreSQL is running
- Review connection pool settings

**Email sending fails**
- Verify SMTP credentials
- Check firewall rules
- Test SMTP connection manually
- Review email logs

**High memory usage**
- Check for memory leaks
- Review JVM settings
- Analyze heap dumps
- Optimize queries

## Support

### Documentation
- README.md - Project overview
- API_GUIDE.md - API usage
- ARCHITECTURE.md - System architecture

### Logs Location
- Application: `/var/log/scholar/` or `logs/` directory
- System: `journalctl -u scholar`
- Docker: `docker logs scholar-backend`

### Useful Commands
```bash
# Check service status
sudo systemctl status scholar

# View recent logs
sudo journalctl -u scholar -n 100

# Restart service
sudo systemctl restart scholar

# Check database
psql -U scholar_user -d scholar_db -c "SELECT version();"

# Monitor resources
top
htop
docker stats
```

## Production Checklist Summary

- [x] All phases completed (1-11)
- [x] Database schema created
- [x] Application configured
- [x] Security implemented
- [x] APIs documented
- [ ] Production environment configured
- [ ] Backups configured
- [ ] Monitoring set up
- [ ] Load testing performed
- [ ] Security audit completed

## Go-Live Approval

- [ ] Development team approval
- [ ] QA team approval
- [ ] Security team approval
- [ ] Operations team approval
- [ ] Stakeholder approval

---

**Deployment Date**: _______________

**Deployed By**: _______________

**Version**: 1.0.0

**Status**: _______________
