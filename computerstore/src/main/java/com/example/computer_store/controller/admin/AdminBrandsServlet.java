package com.example.computer_store.controller.admin;

import com.example.computer_store.exception.NotFoundException;
import com.example.computer_store.exception.ValidationException;
import com.example.computer_store.model.Brand;
import com.example.computer_store.service.BrandService;
import com.example.computer_store.util.Flash;
import com.example.computer_store.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Admin brand CRUD:
 *   GET  /admin/brands?edit=ID          -> edit form (same page)
 *   GET  /admin/brands?delete=ID        -> delete
 *   POST /admin/brands                  -> create / update
 */
@WebServlet("/admin/brands")
public class AdminBrandsServlet extends HttpServlet {

    private final BrandService brandService = new BrandService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer editId = ValidationUtil.parseInt(request.getParameter("edit"));
        if (editId != null) {
            try {
                request.setAttribute("editing", brandService.get(editId));
            } catch (NotFoundException e) {
                Flash.error(request, e.getMessage());
                response.sendRedirect(request.getContextPath() + "/admin/brands");
                return;
            }
        }
        Integer deleteId = ValidationUtil.parseInt(request.getParameter("delete"));
        if (deleteId != null) {
            try {
                brandService.delete(deleteId);
                Flash.success(request, "Brand deleted.");
            } catch (ValidationException e) {
                Flash.error(request, e.getMessage());
            }
            response.sendRedirect(request.getContextPath() + "/admin/brands");
            return;
        }
        request.setAttribute("brands", brandService.getAll());
        request.getRequestDispatcher("/WEB-INF/views/admin/brands.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer brandId = ValidationUtil.parseInt(request.getParameter("brandId"));
        String name = request.getParameter("name");
        String description = request.getParameter("description");

        try {
            if (brandId == null) {
                brandService.create(name, description);
                Flash.success(request, "Brand created successfully.");
            } else {
                brandService.update(brandId, name, description);
                Flash.success(request, "Brand updated successfully.");
            }
            response.sendRedirect(request.getContextPath() + "/admin/brands");
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            if (brandId != null) {
                request.setAttribute("editing", brandService.get(brandId) != null ? brandService.get(brandId) : null);
            }
            request.setAttribute("brands", brandService.getAll());
            request.getRequestDispatcher("/WEB-INF/views/admin/brands.jsp").forward(request, response);
        }
    }
}