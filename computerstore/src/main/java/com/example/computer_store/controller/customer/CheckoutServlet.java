package com.example.computer_store.controller.customer;

import com.example.computer_store.exception.InsufficientStockException;
import com.example.computer_store.exception.ValidationException;
import com.example.computer_store.model.CartItem;
import com.example.computer_store.model.Order;
import com.example.computer_store.model.User;
import com.example.computer_store.service.CartService;
import com.example.computer_store.service.OrderService;
import com.example.computer_store.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/checkout")
public class CheckoutServlet extends HttpServlet {

    private final CartService cartService = new CartService();
    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = sessionUser(request);
        List<CartItem> items = cartService.getCartItems(user.getUserId());
        if (items.isEmpty()) {
            Flash.error(request, "Your cart is empty. Add some products first.");
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }
        request.setAttribute("items", items);
        request.setAttribute("total", cartService.getTotal(items));
        request.getRequestDispatcher("/WEB-INF/views/customer/checkout.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = sessionUser(request);
        try {
            Order order = orderService.checkout(user);
            request.getSession().setAttribute("flashMessage",
                    "Order #" + order.getOrderId() + " placed successfully.");
            request.getSession().setAttribute("flashType", "success");
            response.sendRedirect(request.getContextPath() + "/account/orders?id=" + order.getOrderId());
        } catch (InsufficientStockException | ValidationException e) {
            Flash.error(request, e.getMessage());
            response.sendRedirect(request.getContextPath() + "/cart");
        }
    }

    private User sessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }
}