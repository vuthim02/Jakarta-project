package com.example.computer_store.controller.customer;

import com.example.computer_store.exception.NotFoundException;
import com.example.computer_store.exception.ValidationException;
import com.example.computer_store.model.User;
import com.example.computer_store.service.CartService;
import com.example.computer_store.util.Flash;
import com.example.computer_store.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import com.example.computer_store.model.CartItem;

@WebServlet({"/cart", "/cart/add", "/cart/update", "/cart/remove"})
public class CartServlet extends HttpServlet {

    private final CartService cartService = new CartService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = sessionUser(request);
        List<CartItem> items = cartService.getCartItems(user.getUserId());
        request.setAttribute("items", items);
        request.setAttribute("total", cartService.getTotal(items));
        request.getRequestDispatcher("/WEB-INF/views/customer/cart.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        try {
            switch (path) {
                case "/cart/add" -> {
                    int productId = requireInt(request, "productId");
                    int quantity = optionalInt(request, "quantity", 1);
                    cartService.add(sessionUser(request).getUserId(), productId, quantity);
                    Flash.success(request, "Item added to your cart.");
                }
                case "/cart/update" -> {
                    int cartItemId = requireInt(request, "cartItemId");
                    int quantity = requireInt(request, "quantity");
                    cartService.updateQuantity(sessionUser(request).getUserId(), cartItemId, quantity);
                    Flash.success(request, "Cart updated.");
                }
                case "/cart/remove" -> {
                    int cartItemId = requireInt(request, "cartItemId");
                    cartService.remove(sessionUser(request).getUserId(), cartItemId);
                    Flash.success(request, "Item removed from your cart.");
                }
                default -> throw new NotFoundException("Unknown cart action.");
            }
        } catch (ValidationException | NotFoundException e) {
            Flash.error(request, e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/cart");
    }

    private User sessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }

    private int requireInt(HttpServletRequest request, String param) {
        Integer value = ValidationUtil.parseInt(request.getParameter(param));
        if (value == null) {
            throw new ValidationException("Invalid request.");
        }
        return value;
    }

    private int optionalInt(HttpServletRequest request, String param, int defaultValue) {
        Integer value = ValidationUtil.parseInt(request.getParameter(param));
        return value == null ? defaultValue : value;
    }
}