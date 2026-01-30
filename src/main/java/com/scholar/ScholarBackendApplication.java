package com.scholar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application entry point for the Scholar Backend system.
 * 
 * This enterprise-grade application provides:
 * - Multi-tenant CV management and parsing
 * - Intelligent keyword matching with professors
 * - Automated email outreach campaigns
 * - Secure SMTP account management
 * 
 * @author Scholar Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
public class ScholarBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScholarBackendApplication.class, args);
    }
}
