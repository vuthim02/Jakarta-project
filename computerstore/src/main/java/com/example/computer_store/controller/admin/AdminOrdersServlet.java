package com.example.computer_store.controller.admin;

import com.example.computer_store.exception.NotFoundException;
import com.example.computer_store.model.Order;
import com.example.computer_store.service.OrderService;
import com.example.computer_store.util.Flash;
import com.example.computer_store.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Admin order management:
 *   GET  /admin/orders            -> order list
 *   GET  /admin/orders?id=ID      -> order detail
 *   POST /admin/orders            -> update order status
 */
@WebServlet("/admin/orders")
public class AdminOrdersServlet extends HttpServlet {

    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer orderId = ValidationUtil.parseInt(request.getParameter("id"));
        if (orderId != null) {
            try {
                Order order = orderService.getOrder(orderId);
                request.setAttribute("order", order);
                request.getRequestDispatcher("/WEB-INF/views/admin/order-detail.jsp")
                        .forward(request, response);
            } catch (NotFoundException e) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            return;
        }
        request.setAttribute("orders", orderService.getAllOrders());
        request.getRequestDispatcher("/WEB-INF/views/admin/orders.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer orderId = ValidationUtil.parseInt(request.getParameter("orderId"));
        String statusParam = request.getParameter("status");
        if (orderId == null || statusParam == null) {
            Flash.error(request, "Invalid request.");
            response.sendRedirect(request.getContextPath() + "/admin/orders");
            return;
        }
        try {
            Order.Status status = Order.Status.valueOf(statusParam);
            orderService.updateStatus(orderId, status);
            Flash.success(request, "Order #" + orderId + " status changed to " + status.name() + ".");
        } catch (IllegalArgumentException e) {
            Flash.error(request, "Unknown order status.");
        }
        response.sendRedirect(request.getContextPath() + "/admin/orders?id=" + orderId);
    }
}