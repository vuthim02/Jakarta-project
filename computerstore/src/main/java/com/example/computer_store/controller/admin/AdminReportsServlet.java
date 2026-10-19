package com.example.computer_store.controller.admin;

import com.example.computer_store.service.ReportService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/admin/reports")
public class AdminReportsServlet extends HttpServlet {

    private final ReportService reportService = new ReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("summary", reportService.getSalesSummary());
        request.setAttribute("stockValues", reportService.getLowStockValues());
        request.setAttribute("lowStock", reportService.getLowStock());
        request.setAttribute("outOfStock", reportService.getOutOfStock());
        request.getRequestDispatcher("/WEB-INF/views/admin/reports.jsp").forward(request, response);
    }
}