package com.example.computer_store.filter;

import com.example.computer_store.model.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Protects every protected URL (admin, cart, checkout, account pages).
 * Unauthenticated users are redirected to the login page.
 */
public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            String returnPath = request.getRequestURI();
            if (request.getQueryString() != null) {
                returnPath += "?" + request.getQueryString();
            }
            response.sendRedirect(request.getContextPath() + "/login?return="
                    + URLEncoder.encode(returnPath, StandardCharsets.UTF_8));
            return;
        }
        chain.doFilter(request, response);
    }
}