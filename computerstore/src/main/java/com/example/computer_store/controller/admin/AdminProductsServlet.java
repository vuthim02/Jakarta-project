package com.example.computer_store.controller.admin;

import com.example.computer_store.exception.NotFoundException;
import com.example.computer_store.exception.ValidationException;
import com.example.computer_store.model.Product;
import com.example.computer_store.service.BrandService;
import com.example.computer_store.service.CategoryService;
import com.example.computer_store.service.ProductService;
import com.example.computer_store.util.Flash;
import com.example.computer_store.util.FileUploadUtil;
import com.example.computer_store.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Admin product management:
 *   GET  /admin/products               -> product table
 *   GET  /admin/products?new           -> create form
 *   GET  /admin/products?edit=ID       -> edit form
 *   GET  /admin/products?delete=ID     -> delete / discontinue
 *   POST /admin/products               -> create or update
 */
@WebServlet("/admin/products")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,      // 1 MB
    maxFileSize = 1024 * 1024 * 5,      // 5 MB
    maxRequestSize = 1024 * 1024 * 10   // 10 MB
)
public class AdminProductsServlet extends HttpServlet {

    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();
    private final BrandService brandService = new BrandService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("new".equals(action)) {
            forwardForm(request, response, null);
            return;
        }
        Integer editId = ValidationUtil.parseInt(request.getParameter("edit"));
        if (editId != null) {
            forwardForm(request, response, productService.get(editId));
            return;
        }
        Integer deleteId = ValidationUtil.parseInt(request.getParameter("delete"));
        if (deleteId != null) {
            try {
                boolean deleted = productService.delete(deleteId);
                if (deleted) {
                    Flash.success(request, "Product deleted.");
                } else {
                    Flash.success(request, "Product has order history and was marked as DISCONTINUED instead.");
                }
            } catch (ValidationException e) {
                Flash.error(request, e.getMessage());
            }
            response.sendRedirect(request.getContextPath() + "/admin/products");
            return;
        }
        request.setAttribute("products", productService.getAll());
        request.getRequestDispatcher("/WEB-INF/views/admin/products.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer productId = ValidationUtil.parseInt(request.getParameter("productId"));
        Integer categoryId = ValidationUtil.parseInt(request.getParameter("categoryId"));
        Integer brandId = ValidationUtil.parseInt(request.getParameter("brandId"));
        String name = request.getParameter("name");
        String sku = request.getParameter("sku");
        String description = request.getParameter("description");
        BigDecimal price = ValidationUtil.parseDecimal(request.getParameter("price"));
        int stock = optionalInt(request.getParameter("stockQuantity"), 0);

        try {
            // Handle file upload
            Part filePart = request.getPart("productImage");
            String uploadPath = getServletContext().getRealPath("/");
            String imageUrl = null;
            boolean imageUploadFailed = false;
            
            try {
                imageUrl = FileUploadUtil.saveUploadedFile(filePart, uploadPath);
            } catch (IOException e) {
                // If file upload fails, continue without image
                imageUploadFailed = true;
            }

            // Get existing image URL if no new file uploaded and editing
            if (imageUrl == null && productId != null) {
                try {
                    Product existing = productService.get(productId);
                    imageUrl = existing.getImageUrl();
                } catch (NotFoundException ignored) {
                    // Product doesn't exist, continue with null image
                }
            }

            if (productId == null) {
                productService.create(categoryId, brandId, name, sku, description, price, stock, imageUrl);
                if (imageUploadFailed) {
                    Flash.warning(request, "Product created but image upload failed. Please edit the product to add an image.");
                } else {
                    Flash.success(request, "Product created successfully.");
                }
            } else {
                productService.update(productId, categoryId, brandId, name, sku, description, price, stock, imageUrl);
                if (imageUploadFailed) {
                    Flash.warning(request, "Product updated but image upload failed. Please try again.");
                } else {
                    Flash.success(request, "Product updated successfully.");
                }
            }
            response.sendRedirect(request.getContextPath() + "/admin/products");
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            forwardForm(request, response, buildTemporaryProduct(request, productId));
        } catch (Exception e) {
            request.setAttribute("error", "An unexpected error occurred: " + e.getMessage());
            forwardForm(request, response, buildTemporaryProduct(request, productId));
        }
    }

    private void forwardForm(HttpServletRequest request, HttpServletResponse response,
                             Product product) throws ServletException, IOException {
        if (product != null) {
            request.setAttribute("product", product);
            request.setAttribute("selectedCategoryId", product.getCategoryId());
            request.setAttribute("selectedBrandId", product.getBrandId());
            // Add context path for image URL display
            if (product.getImageUrl() != null) {
                request.setAttribute("currentImageUrl", 
                    request.getContextPath() + "/" + product.getImageUrl());
            }
        }
        request.setAttribute("categories", categoryService.getAll());
        request.setAttribute("brands", brandService.getAll());
        request.getRequestDispatcher("/WEB-INF/views/admin/product-form.jsp").forward(request, response);
    }

    private Product buildTemporaryProduct(HttpServletRequest request, Integer productId) {
        Product p = new Product();
        if (productId != null) {
            p.setProductId(productId);
        }
        p.setCategoryId(optionalInt(request.getParameter("categoryId"), 0));
        p.setBrandId(optionalInt(request.getParameter("brandId"), 0));
        p.setName(request.getParameter("name"));
        p.setSku(request.getParameter("sku"));
        p.setDescription(request.getParameter("description"));
        p.setPrice(ValidationUtil.parseDecimal(request.getParameter("price")));
        p.setStockQuantity(optionalInt(request.getParameter("stockQuantity"), 0));
        return p;
    }

    private int optionalInt(String value, int defaultValue) {
        Integer parsed = ValidationUtil.parseInt(value);
        return parsed == null ? defaultValue : parsed;
    }
}