package com.shopmate.agent.repository;

import com.shopmate.agent.model.KnowledgeArticle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class KnowledgeRepository {

    private static final RowMapper<KnowledgeArticle> MAPPER = (rs, rowNum) -> new KnowledgeArticle(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("category"),
            rs.getString("content"));

    private final JdbcTemplate jdbc;

    public KnowledgeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<KnowledgeArticle> findAll() {
        return jdbc.query("SELECT * FROM kb_article ORDER BY id", MAPPER);
    }
}