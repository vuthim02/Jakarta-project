package com.example.computer_store.service;

import com.example.computer_store.dao.CartDAO;
import com.example.computer_store.dao.ProductDAO;
import com.example.computer_store.exception.NotFoundException;
import com.example.computer_store.exception.ValidationException;
import com.example.computer_store.model.CartItem;
import com.example.computer_store.model.Product;

import java.math.BigDecimal;
import java.util.List;

public class CartService {

    private final CartDAO cartDAO = new CartDAO();
    private final ProductDAO productDAO = new ProductDAO();

    public List<CartItem> getCartItems(int userId) {
        return cartDAO.findItemsByUser(userId);
    }

    public int countItems(int userId) {
        if (userId <= 0) {
            return 0;
        }
        return cartDAO.countItems(userId);
    }

    public BigDecimal getTotal(List<CartItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        if (items == null) {
            return total;
        }
        for (CartItem item : items) {
            if (item.getProduct() != null && item.getProduct().getPrice() != null) {
                total = total.add(item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        return total;
    }

    public void add(int userId, int productId, int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("Quantity must be at least 1.");
        }
        Product product = productDAO.findById(productId);
        if (product == null) {
            throw new NotFoundException("Product does not exist.");
        }
        if (product.getStatus() == Product.Status.DISCONTINUED) {
            throw new ValidationException("This product is no longer available.");
        }
        if (product.getStockQuantity() <= 0) {
            throw new ValidationException("This product is out of stock.");
        }

        CartItem existing = cartDAO.findItem(userId, productId);
        int target = (existing == null ? 0 : existing.getQuantity()) + quantity;
        if (target > product.getStockQuantity()) {
            throw new ValidationException("Only " + product.getStockQuantity()
                    + " units of this product are available.");
        }
        cartDAO.saveItem(userId, productId, target);
    }

    public void updateQuantity(int userId, int cartItemId, int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("Quantity must be at least 1.");
        }
        CartItem item = cartDAO.findItemsByUser(userId).stream()
                .filter(i -> i.getCartItemId() == cartItemId)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Cart item does not exist."));
        if (quantity > item.getProduct().getStockQuantity()) {
            throw new ValidationException("Only " + item.getProduct().getStockQuantity()
                    + " units of this product are available.");
        }
        cartDAO.updateQuantity(cartItemId, userId, quantity);
    }

    public void remove(int userId, int cartItemId) {
        cartDAO.removeItem(cartItemId, userId);
    }

    public void clear(int userId) {
        cartDAO.clear(userId);
    }
}