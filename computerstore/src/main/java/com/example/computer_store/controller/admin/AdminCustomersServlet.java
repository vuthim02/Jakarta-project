package com.example.computer_store.controller.admin;

import com.example.computer_store.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/admin/customers")
public class AdminCustomersServlet extends HttpServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("customers", userService.getCustomers());
        request.getRequestDispatcher("/WEB-INF/views/admin/customers.jsp").forward(request, response);
    }
}