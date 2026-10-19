package com.example.computer_store.dao;

import com.example.computer_store.model.Product;
import com.example.computer_store.util.DBConnection;
import com.example.computer_store.util.ErrorHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    private static final String COLUMNS_WITHOUT_IMAGE =
            "p.product_id, p.category_id, p.brand_id, p.name, p.sku, p.description, "
            + "p.price, p.stock_quantity, p.status, p.created_at, p.updated_at, "
            + "c.name AS category_name, b.name AS brand_name";

    private static final String COLUMNS_WITH_IMAGE =
            "p.product_id, p.category_id, p.brand_id, p.name, p.sku, p.description, "
            + "p.price, p.stock_quantity, p.image_url, p.status, p.created_at, p.updated_at, "
            + "c.name AS category_name, b.name AS brand_name";

    private static String COLUMNS;
    private static boolean schemaHasImageColumn = false;

    private static final String FROM_JOINS =
            "FROM products p "
            + "JOIN categories c ON c.category_id = p.category_id "
            + "JOIN brands b ON b.brand_id = p.brand_id ";

    static {
        // Detect if image_url column exists in database
        Connection c = null;
        try {
            c = DBConnection.getConnection();
            DatabaseMetaData meta = c.getMetaData();
            ResultSet columns = meta.getColumns(null, null, "products", "image_url");
            if (columns.next()) {
                COLUMNS = COLUMNS_WITH_IMAGE;
                schemaHasImageColumn = true;
            } else {
                COLUMNS = COLUMNS_WITHOUT_IMAGE;
                schemaHasImageColumn = false;
            }
            columns.close();
        } catch (SQLException e) {
            // Default to old schema if detection fails
            COLUMNS = COLUMNS_WITHOUT_IMAGE;
            schemaHasImageColumn = false;
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e) {
                    // Ignore close error
                }
            }
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getInt("product_id"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setBrandId(rs.getInt("brand_id"));
        p.setName(rs.getString("name"));
        p.setSku(rs.getString("sku"));
        p.setDescription(rs.getString("description"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setStockQuantity(rs.getInt("stock_quantity"));
        // Safely handle image_url column that may not exist in older schemas
        try {
            p.setImageUrl(rs.getString("image_url"));
        } catch (SQLException e) {
            p.setImageUrl(null); // Column doesn't exist
        }
        p.setStatus(Product.Status.valueOf(rs.getString("status")));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));
        p.setCategoryName(rs.getString("category_name"));
        p.setBrandName(rs.getString("brand_name"));
        return p;
    }

    private Connection conn() throws SQLException {
        return DBConnection.getConnection();
    }

    /**
     * Customer-facing search with optional filters. Only non-discontinued
     * products are returned.
     */
    public List<Product> search(String search, Integer categoryId, Integer brandId,
                                BigDecimal minPrice, BigDecimal maxPrice) {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " " + FROM_JOINS + " WHERE p.status <> 'DISCONTINUED' ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (p.name LIKE ? OR p.sku LIKE ? OR b.name LIKE ?) ");
            String like = "%" + search.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (categoryId != null) {
            sql.append("AND p.category_id = ? ");
            params.add(categoryId);
        }
        if (brandId != null) {
            sql.append("AND p.brand_id = ? ");
            params.add(brandId);
        }
        if (minPrice != null) {
            sql.append("AND p.price >= ? ");
            params.add(minPrice);
        }
        if (maxPrice != null) {
            sql.append("AND p.price <= ? ");
            params.add(maxPrice);
        }
        sql.append("ORDER BY p.name");

        return queryList(sql.toString(), params);
    }

    public List<Product> findAll() {
        String sql = "SELECT " + COLUMNS + " " + FROM_JOINS + " ORDER BY p.name";
        return queryList(sql, List.of());
    }

    public Product findById(int productId) {
        String sql = "SELECT " + COLUMNS + " " + FROM_JOINS + " WHERE p.product_id = ?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw ErrorHandler.handleDatabaseError("finding product", e);
        }
        return null;
    }

    public Product findBySku(String sku) {
        String sql = "SELECT " + COLUMNS + " " + FROM_JOINS + " WHERE p.sku = ?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw ErrorHandler.handleDatabaseError("finding product by sku", e);
        }
        return null;
    }

    public int create(Product product) {
        String sql;
        if (schemaHasImageColumn) {
            sql = "INSERT INTO products (category_id, brand_id, name, sku, description, price, stock_quantity, image_url, status) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            sql = "INSERT INTO products (category_id, brand_id, name, sku, description, price, stock_quantity, status) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        }
        
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, product.getCategoryId());
            ps.setInt(2, product.getBrandId());
            ps.setString(3, product.getName());
            ps.setString(4, product.getSku());
            ps.setString(5, product.getDescription());
            ps.setBigDecimal(6, product.getPrice());
            ps.setInt(7, product.getStockQuantity());
            
            if (schemaHasImageColumn) {
                ps.setString(8, product.getImageUrl());
                ps.setString(9, product.getStatus().name());
            } else {
                ps.setString(8, product.getStatus().name());
            }
            
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            throw ErrorHandler.handleDatabaseError("creating product", e);
        }
    }

    public void update(Product product) {
        String sql;
        if (schemaHasImageColumn) {
            sql = "UPDATE products SET category_id = ?, brand_id = ?, name = ?, sku = ?, "
                    + "description = ?, price = ?, stock_quantity = ?, image_url = ?, status = ? WHERE product_id = ?";
        } else {
            sql = "UPDATE products SET category_id = ?, brand_id = ?, name = ?, sku = ?, "
                    + "description = ?, price = ?, stock_quantity = ?, status = ? WHERE product_id = ?";
        }
        
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, product.getCategoryId());
            ps.setInt(2, product.getBrandId());
            ps.setString(3, product.getName());
            ps.setString(4, product.getSku());
            ps.setString(5, product.getDescription());
            ps.setBigDecimal(6, product.getPrice());
            ps.setInt(7, product.getStockQuantity());
            
            if (schemaHasImageColumn) {
                ps.setString(8, product.getImageUrl());
                ps.setString(9, product.getStatus().name());
                ps.setInt(10, product.getProductId());
            } else {
                ps.setString(8, product.getStatus().name());
                ps.setInt(9, product.getProductId());
            }
            
            ps.executeUpdate();
        } catch (SQLException e) {
            throw ErrorHandler.handleDatabaseError("updating product", e);
        }
    }

    /**
     * A product cannot be removed when it has order history records.
     * Returns true when the row was actually deleted.
     */
    public boolean delete(int productId) {
        String checkSql = "SELECT COUNT(*) FROM order_items WHERE product_id = ?";
        try (Connection c = conn()) {
            boolean referenced;
            try (PreparedStatement ps = c.prepareStatement(checkSql)) {
                ps.setInt(1, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    referenced = rs.getInt(1) > 0;
                }
            }
            if (referenced) {
                return false;
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM cart_items WHERE product_id = ?")) {
                ps.setInt(1, productId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM products WHERE product_id = ?")) {
                ps.setInt(1, productId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw ErrorHandler.handleDatabaseError("deleting product", e);
        }
    }

    /**
     * Atomically decreases stock when the available quantity is sufficient.
     * Returns false when requested quantity exceeds the current stock.
     */
    public boolean reduceStock(Connection c, int productId, int quantity) throws SQLException {
        String sql = "UPDATE products SET stock_quantity = stock_quantity - ? "
                + "WHERE product_id = ? AND stock_quantity >= ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);
            return ps.executeUpdate() > 0;
        }
    }

    public void setStock(Connection c, int productId, int newQuantity,
                         Product.Status status) throws SQLException {
        String sql = "UPDATE products SET stock_quantity = ?, status = ? WHERE product_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, newQuantity);
            ps.setString(2, status.name());
            ps.setInt(3, productId);
            ps.executeUpdate();
        }
    }

    public void markDiscontinued(Connection c, int productId) throws SQLException {
        String sql = "UPDATE products SET status = 'DISCONTINUED' WHERE product_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        }
    }

    public long countAll() {
        String sql = "SELECT COUNT(*) FROM products";
        return countScalar(sql);
    }

    public long countStockThreshold(boolean outOfStock, int lowStockThreshold) {
        String sql = outOfStock
                ? "SELECT COUNT(*) FROM products WHERE status <> 'DISCONTINUED' AND stock_quantity = 0"
                : "SELECT COUNT(*) FROM products WHERE status <> 'DISCONTINUED' AND stock_quantity > 0 AND stock_quantity <= ?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (!outOfStock) {
                ps.setInt(1, lowStockThreshold);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw ErrorHandler.handleDatabaseError("counting stock", e);
        }
        return 0;
    }

    public java.math.BigDecimal totalStockValue() {
        String sql = "SELECT COALESCE(SUM(price * stock_quantity), 0) FROM products "
                + "WHERE status <> 'DISCONTINUED'";
        return scalarDecimal(sql);
    }

    public java.math.BigDecimal lowStockValue(int lowStockThreshold) {
        String sql = "SELECT COALESCE(SUM(price * stock_quantity), 0) FROM products "
                + "WHERE status <> 'DISCONTINUED' AND stock_quantity > 0 AND stock_quantity <= ?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, lowStockThreshold);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        } catch (SQLException e) {
            throw ErrorHandler.handleDatabaseError("computing low stock value", e);
        }
        return java.math.BigDecimal.ZERO;
    }

    private java.math.BigDecimal scalarDecimal(String sql) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw ErrorHandler.handleDatabaseError("executing aggregate query", e);
        }
        return java.math.BigDecimal.ZERO;
    }

    private long countScalar(String sql) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw ErrorHandler.handleDatabaseError("executing count query", e);
        }
        return 0;
    }

    private List<Product> queryList(String sql, List<Object> params) {
        List<Product> list = new ArrayList<>();
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw ErrorHandler.handleDatabaseError("executing product query", e);
        }
        return list;
    }
}