package com.shopmate.agent.repository;

import com.shopmate.agent.model.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepository {

    private static final RowMapper<Product> MAPPER = (rs, rowNum) -> new Product(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("brand"),
            rs.getString("category"),
            rs.getBigDecimal("price"),
            rs.getBigDecimal("original_price"),
            rs.getBigDecimal("rating"),
            rs.getInt("stock"),
            splitTags(rs.getString("tags")),
            rs.getString("summary"));

    private final JdbcTemplate jdbc;

    public ProductRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Product> search(String keyword, String category, Double minPrice, Double maxPrice, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM product WHERE 1 = 1");
        List<Object> args = new ArrayList<>();

        if (hasText(keyword)) {
            sql.append(" AND (LOWER(name) LIKE ? OR LOWER(brand) LIKE ? OR LOWER(tags) LIKE ? OR LOWER(summary) LIKE ?)");
            String like = "%" + keyword.trim().toLowerCase() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (hasText(category)) {
            sql.append(" AND category = ?");
            args.add(category.trim());
        }
        if (minPrice != null) {
            sql.append(" AND price >= ?");
            args.add(minPrice);
        }
        if (maxPrice != null) {
            sql.append(" AND price <= ?");
            args.add(maxPrice);
        }
        sql.append(" ORDER BY rating DESC, price ASC LIMIT ?");
        args.add(Math.max(1, Math.min(limit, 50)));

        return jdbc.query(sql.toString(), MAPPER, args.toArray());
    }

    public List<Product> findAll() {
        return jdbc.query("SELECT * FROM product ORDER BY category, id", MAPPER);
    }

    public Optional<Product> findById(long id) {
        return jdbc.query("SELECT * FROM product WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    public List<String> categories() {
        return jdbc.queryForList("SELECT DISTINCT category FROM product ORDER BY category", String.class);
    }

    private static List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}