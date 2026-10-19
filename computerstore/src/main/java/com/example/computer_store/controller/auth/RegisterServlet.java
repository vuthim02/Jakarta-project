package com.example.computer_store.controller.auth;

import com.example.computer_store.exception.ValidationException;
import com.example.computer_store.model.User;
import com.example.computer_store.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (sessionUser(request) != null) {
            response.sendRedirect(request.getContextPath() + "/products");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        try {
            User user = authService.register(username, fullName, email, password, confirmPassword);
            HttpSession session = request.getSession();
            session.setAttribute("user", user);
            session.setAttribute("flashMessage", "Welcome, " + user.getFullName() + "! Your account was created.");
            session.setAttribute("flashType", "success");
            response.sendRedirect(request.getContextPath() + "/products");
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("username", username);
            request.setAttribute("fullName", fullName);
            request.setAttribute("email", email);
            request.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(request, response);
        }
    }

    private User sessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }
}