package com.example.computer_store.controller.auth;

import com.example.computer_store.exception.ValidationException;
import com.example.computer_store.model.User;
import com.example.computer_store.service.AuthService;
import com.example.computer_store.util.CSRFUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (sessionUser(request) != null) {
            response.sendRedirect(request.getContextPath() + "/products");
            return;
        }
        if ("1".equals(request.getParameter("logout"))) {
            request.setAttribute("info", "You have been logged out.");
        }
        // Generate CSRF token for the login form
        HttpSession session = request.getSession(true);
        request.setAttribute("csrfToken", CSRFUtil.generateToken(session));
        forwardLogin(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String returnPath = request.getParameter("return");

        try {
            User user = authService.login(username, password, request);
            HttpSession session = request.getSession();
            session.setAttribute("user", user);
            // Generate new CSRF token after successful login
            CSRFUtil.generateToken(session);

            String target = user.isAdmin() ? "/admin" : "/products";
            if (isSafeReturnPath(returnPath, request)) {
                target = returnPath;
            }
            response.sendRedirect(request.getContextPath() + target);
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("username", username);
            // Regenerate CSRF token on failed login
            HttpSession session = request.getSession(true);
            request.setAttribute("csrfToken", CSRFUtil.generateToken(session));
            forwardLogin(request, response);
        }
    }

    private void forwardLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(request, response);
    }

    private User sessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }

    private boolean isSafeReturnPath(String path, HttpServletRequest request) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        try {
            URI uri = new URI(path);
            String host = uri.getHost();
            
            // Only allow relative URLs or same-host redirects
            if (host == null) {
                // Relative URL - check it starts with /
                return path.startsWith("/") && !path.startsWith("//");
            } else {
                // Absolute URL - must match server host
                return host.equals(request.getServerName()) && 
                       (uri.getScheme() == null || 
                        uri.getScheme().equals(request.getScheme()));
            }
        } catch (URISyntaxException e) {
            return false;
        }
    }
}