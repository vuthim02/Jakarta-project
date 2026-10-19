package com.example.computer_store.service;

import com.example.computer_store.dao.UserDAO;
import com.example.computer_store.exception.NotFoundException;
import com.example.computer_store.exception.ValidationException;
import com.example.computer_store.model.User;
import com.example.computer_store.util.PasswordUtil;
import com.example.computer_store.util.ValidationUtil;

import java.util.List;

public class UserService {

    private final UserDAO userDAO = new UserDAO();

    public User get(int userId) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new NotFoundException("User does not exist.");
        }
        user.setPasswordHash(null);
        return user;
    }

    public List<User> getCustomers() {
        return userDAO.findByRole(User.Role.CUSTOMER);
    }

    public List<User> getAdmins() {
        List<User> admins = userDAO.findByRole(User.Role.ADMIN);
        admins.forEach(a -> a.setPasswordHash(null));
        return admins;
    }

    public void updateProfile(int userId, String fullName, String email) {
        if (ValidationUtil.isBlank(fullName)) {
            throw new ValidationException("Full name is required.");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new ValidationException("Please enter a valid email address.");
        }
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new NotFoundException("User does not exist.");
        }
        User emailOwner = userDAO.findByEmail(email.trim());
        if (emailOwner != null && emailOwner.getUserId() != userId) {
            throw new ValidationException("An account with this email already exists.");
        }
        user.setFullName(fullName.trim());
        user.setEmail(email.trim());
        userDAO.update(user);
    }

    public void changePassword(int userId, String currentPassword, String newPassword) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new NotFoundException("User does not exist.");
        }
        if (ValidationUtil.isBlank(currentPassword) || ValidationUtil.isBlank(newPassword)) {
            throw new ValidationException("Both passwords are required.");
        }
        if (!PasswordUtil.check(currentPassword, user.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect.");
        }
        if (newPassword.length() < 6) {
            throw new ValidationException("New password must be at least 6 characters.");
        }
        userDAO.updatePassword(userId, PasswordUtil.hash(newPassword));
    }
}