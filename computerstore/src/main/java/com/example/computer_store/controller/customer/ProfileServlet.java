package com.example.computer_store.controller.customer;

import com.example.computer_store.exception.ValidationException;
import com.example.computer_store.model.User;
import com.example.computer_store.service.UserService;
import com.example.computer_store.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/account/profile")
public class ProfileServlet extends HttpServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/customer/profile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = sessionUser(request);
        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");

        try {
            userService.updateProfile(user.getUserId(), fullName, email);
            user.setFullName(fullName.trim());
            user.setEmail(email.trim());

            if (currentPassword != null && !currentPassword.isEmpty()) {
                userService.changePassword(user.getUserId(), currentPassword, newPassword);
            }
            Flash.success(request, "Profile updated successfully.");
        } catch (ValidationException e) {
            Flash.error(request, e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/account/profile");
    }

    private User sessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }
}