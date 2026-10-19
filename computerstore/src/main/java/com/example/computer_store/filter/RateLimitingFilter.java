package com.example.computer_store.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limiting Filter.
 * Prevents brute force attacks and DoS by limiting request rates per IP address.
 */
@WebFilter({"/login", "/register"})
public class RateLimitingFilter implements Filter {

    // Store request counts and timestamps per IP
    private static final ConcurrentHashMap<String, RateLimitInfo> rateLimits = new ConcurrentHashMap<>();
    
    // Rate limiting configuration
    private static final int MAX_REQUESTS_PER_MINUTE = 5;
    private static final int MAX_REQUESTS_PER_HOUR = 20;
    private static final long CLEANUP_INTERVAL_MS = 60_000; // 1 minute
    
    static class RateLimitInfo {
        AtomicInteger minuteCount = new AtomicInteger(0);
        AtomicInteger hourCount = new AtomicInteger(0);
        long lastResetTime = System.currentTimeMillis();
        
        boolean isExpired() {
            return System.currentTimeMillis() - lastResetTime > CLEANUP_INTERVAL_MS;
        }
        
        void reset() {
            minuteCount.set(0);
            hourCount.set(0);
            lastResetTime = System.currentTimeMillis();
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String clientIp = getClientIp(request);
        
        // Clean up expired entries periodically
        cleanupExpiredEntries();
        
        // Check rate limits
        if (!checkRateLimit(clientIp)) {
            response.sendError(429, // HTTP 429 Too Many Requests
                    "Too many requests. Please try again later.");
            return;
        }
        
        chain.doFilter(servletRequest, servletResponse);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Handle multiple IPs in X-Forwarded-For (take the first one)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private boolean checkRateLimit(String ip) {
        RateLimitInfo info = rateLimits.computeIfAbsent(ip, k -> new RateLimitInfo());
        
        // Reset counters if expired
        if (info.isExpired()) {
            info.reset();
        }
        
        // Check minute limit
        if (info.minuteCount.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
            return false;
        }
        
        // Check hour limit
        if (info.hourCount.incrementAndGet() > MAX_REQUESTS_PER_HOUR) {
            return false;
        }
        
        return true;
    }

    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        rateLimits.entrySet().removeIf(entry -> 
            now - entry.getValue().lastResetTime > CLEANUP_INTERVAL_MS * 60); // Remove after 1 hour
    }
}