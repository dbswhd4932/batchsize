package com.example.batchsize;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Service
public class BulkInsertService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Transactional
    public void bulkInsertWithEntityManager(List<User> users, int batchSize) {
        for (int i = 0; i < users.size(); i++) {
            entityManager.persist(users.get(i));

            if (i > 0 && i % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        entityManager.flush();
        entityManager.clear();
    }

    @Transactional
    public void bulkInsertWithJdbcTemplate(List<User> users) {
        String sql = "INSERT INTO users (name, email, id) VALUES (?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, users, users.size(), (ps, user) -> {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setLong(3, user.getId());
        });
    }

    @Transactional
    public void bulkInsertWithJdbcBatch(List<User> users, int batchSize) throws SQLException {
        String sql = "INSERT INTO users (name, email, id) VALUES (?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            connection.setAutoCommit(false);

            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                ps.setString(1, user.getName());
                ps.setString(2, user.getEmail());
                ps.setLong(3, user.getId());
                ps.addBatch();

                if (i > 0 && i % batchSize == 0) {
                    ps.executeBatch();
                    ps.clearBatch();
                }
            }

            ps.executeBatch();
            connection.commit();
        }
    }

    @Transactional
    public void bulkInsertWithNativeQuery(List<User> users) {
        StringBuilder sql = new StringBuilder("INSERT INTO users (name, email, id) VALUES ");

        for (int i = 0; i < users.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("(?, ?, ?)");
        }

        var query = entityManager.createNativeQuery(sql.toString());

        int paramIndex = 1;
        for (User user : users) {
            query.setParameter(paramIndex++, user.getName());
            query.setParameter(paramIndex++, user.getEmail());
            query.setParameter(paramIndex++, user.getId());
        }

        query.executeUpdate();
    }
}