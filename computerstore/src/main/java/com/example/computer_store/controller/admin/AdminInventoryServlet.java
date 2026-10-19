package com.example.computer_store.controller.admin;

import com.example.computer_store.model.InventoryLog;
import com.example.computer_store.model.Product;
import com.example.computer_store.model.User;
import com.example.computer_store.service.InventoryService;
import com.example.computer_store.service.ProductService;
import com.example.computer_store.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

/**
 * Admin inventory management:
 *   GET  /admin/inventory     -> stock overview, low stock, out of stock, logs
 *   POST /admin/inventory     -> manual stock adjustment
 */
@WebServlet("/admin/inventory")
public class AdminInventoryServlet extends HttpServlet {

    private final InventoryService inventoryService = new InventoryService();
    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("lowStock", inventoryService.getLowStock());
        request.setAttribute("outOfStock", inventoryService.getOutOfStock());
        request.setAttribute("logs", inventoryService.getRecentLogs(15));
        request.setAttribute("products", productService.getAll());
        request.getRequestDispatcher("/WEB-INF/views/admin/inventory.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = sessionUser(request);
        try {
            int productId = Integer.parseInt(request.getParameter("productId"));
            int newQuantity = Integer.parseInt(request.getParameter("newQuantity"));
            inventoryService.adjustStock(productId, newQuantity, user.getUserId());
            Flash.success(request, "Stock updated.");
        } catch (NumberFormatException e) {
            Flash.error(request, "Invalid stock quantity.");
        } catch (RuntimeException e) {
            Flash.error(request, e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/admin/inventory");
    }

    private User sessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }
}