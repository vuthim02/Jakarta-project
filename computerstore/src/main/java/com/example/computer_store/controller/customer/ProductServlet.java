package com.example.computer_store.controller.customer;

import com.example.computer_store.exception.NotFoundException;
import com.example.computer_store.model.Product;
import com.example.computer_store.service.BrandService;
import com.example.computer_store.service.CartService;
import com.example.computer_store.service.CategoryService;
import com.example.computer_store.service.ProductService;
import com.example.computer_store.model.User;
import com.example.computer_store.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Customer-facing product catalogue:
 * GET /products                    -> product listing with filters
 * GET /products?id=10              -> product detail page
 */

@WebServlet("/products")
public class ProductServlet extends HttpServlet {
    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();
    private final BrandService brandService = new BrandService();
    private final CartService cartService = new CartService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer productId = ValidationUtil.parseInt(request.getParameter("id"));
        if (productId != null) {
            showDetail(productId, response, request);
            return;
        }
        showList(request, response);
    }

    private void showDetail(int productId, HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        try {
            Product product = productService.get(productId);
            if (product.getStatus() == Product.Status.DISCONTINUED) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            request.setAttribute("product", product);
            request.setAttribute("categories", categoryService.getAll());
        } catch (NotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        request.getRequestDispatcher("/WEB-INF/views/customer/product-detail.jsp").forward(request, response);
    }

    private void showList(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String search = request.getParameter("search");
        Integer categoryId = ValidationUtil.parseInt(request.getParameter("category"));
        Integer brandId = ValidationUtil.parseInt(request.getParameter("brand"));
        BigDecimal minPrice = ValidationUtil.parseDecimal(request.getParameter("minPrice"));
        BigDecimal maxPrice = ValidationUtil.parseDecimal(request.getParameter("maxPrice"));

        List<Product> products = productService.search(search, categoryId, brandId, minPrice, maxPrice);

        request.setAttribute("products", products);
        request.setAttribute("categories", categoryService.getAll());
        request.setAttribute("brands", brandService.getAll());
        request.setAttribute("cartCount", cartCount(request));
        request.setAttribute("selectedCategoryId", categoryId);
        request.setAttribute("selectedBrandId", brandId);

        request.getRequestDispatcher("/WEB-INF/views/customer/products.jsp").forward(request, response);
    }

    private int cartCount(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        return user == null ? 0 : cartService.countItems(user.getUserId());
    }
}