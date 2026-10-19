package com.example.computer_store.controller.admin;

import com.example.computer_store.exception.NotFoundException;
import com.example.computer_store.exception.ValidationException;
import com.example.computer_store.model.Category;
import com.example.computer_store.service.CategoryService;
import com.example.computer_store.util.Flash;
import com.example.computer_store.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Admin category CRUD:
 *   GET  /admin/categories?edit=ID      -> edit form (same page)
 *   GET  /admin/categories?delete=ID    -> delete
 *   POST /admin/categories               -> create / update
 */
@WebServlet("/admin/categories")
public class AdminCategoriesServlet extends HttpServlet {

    private final CategoryService categoryService = new CategoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer editId = ValidationUtil.parseInt(request.getParameter("edit"));
        if (editId != null) {
            try {
                request.setAttribute("editing", categoryService.get(editId));
            } catch (NotFoundException e) {
                Flash.error(request, e.getMessage());
                response.sendRedirect(request.getContextPath() + "/admin/categories");
                return;
            }
        }
        Integer deleteId = ValidationUtil.parseInt(request.getParameter("delete"));
        if (deleteId != null) {
            try {
                categoryService.delete(deleteId);
                Flash.success(request, "Category deleted.");
            } catch (ValidationException e) {
                Flash.error(request, e.getMessage());
            }
            response.sendRedirect(request.getContextPath() + "/admin/categories");
            return;
        }
        request.setAttribute("categories", categoryService.getAll());
        request.getRequestDispatcher("/WEB-INF/views/admin/categories.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer categoryId = ValidationUtil.parseInt(request.getParameter("categoryId"));
        String name = request.getParameter("name");
        String description = request.getParameter("description");

        try {
            if (categoryId == null) {
                categoryService.create(name, description);
                Flash.success(request, "Category created successfully.");
            } else {
                categoryService.update(categoryId, name, description);
                Flash.success(request, "Category updated successfully.");
            }
            response.sendRedirect(request.getContextPath() + "/admin/categories");
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            if (categoryId != null) {
                request.setAttribute("editing", categoryService.get(categoryId) != null ? categoryService.get(categoryId) : null);
            }
            request.setAttribute("categories", categoryService.getAll());
            request.getRequestDispatcher("/WEB-INF/views/admin/categories.jsp").forward(request, response);
        }
    }
}