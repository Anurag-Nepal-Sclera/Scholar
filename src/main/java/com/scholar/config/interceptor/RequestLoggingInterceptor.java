package com.scholar.config.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final String CSV_FILE = "logs/requests.csv";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public RequestLoggingInterceptor() {
        try {
            Files.createDirectories(Paths.get("logs"));
            if (!Files.exists(Paths.get(CSV_FILE))) {
                try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_FILE))) {
                    writer.println("Timestamp,IP,Method,Endpoint,Status,DurationMs");
                }
            }
        } catch (IOException e) {
            log.error("Failed to initialize CSV log file", e);
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long startTime = (long) request.getAttribute("startTime");
        long duration = System.currentTimeMillis() - startTime;
        
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }

        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();
        String timestamp = LocalDateTime.now().format(formatter);

        // Log to console
        log.info("[API] {} {} {} - {}ms", method, uri, status, duration);

        // Save to CSV
        saveToCsv(timestamp, ipAddress, method, uri, status, duration);
    }

    private synchronized void saveToCsv(String ts, String ip, String method, String uri, int status, long duration) {
        try (FileWriter fw = new FileWriter(CSV_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            // Escape commas in URI if any
            String safeUri = uri.contains(",") ? "\"" + uri + "\"" : uri;
            pw.printf("%s,%s,%s,%s,%d,%d%n", ts, ip, method, safeUri, status, duration);
        } catch (IOException e) {
            log.error("Error writing to CSV log", e);
        }
    }
}
