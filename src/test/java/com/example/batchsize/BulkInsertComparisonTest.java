package com.example.batchsize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.properties.hibernate.jdbc.batch_size=20",
    "spring.jpa.properties.hibernate.order_inserts=true",
    "spring.jpa.properties.hibernate.order_updates=true",
    "spring.jpa.properties.hibernate.batch_versioned_data=true",
    "spring.jpa.show-sql=true",
    "spring.jpa.properties.hibernate.format_sql=true",
    "logging.level.org.hibernate.SQL=DEBUG",
    "logging.level.org.hibernate.engine.jdbc.batch.internal.BatchingBatch=DEBUG"
})
public class BulkInsertComparisonTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BulkInsertService bulkInsertService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    public void testSaveAllMethod() {
        System.out.println("\n=== Testing JPA saveAll() method ===");

        List<User> users = createUsers(25);
        userRepository.saveAll(users);

        System.out.println("=== saveAll() completed - check SQL logs ===\n");
    }

    @Test
    @Transactional
    public void testEntityManagerBulkInsert() {
        System.out.println("\n=== Testing EntityManager bulk insert ===");

        List<User> users = createUsersWithIds(25);
        bulkInsertService.bulkInsertWithEntityManager(users, 10);

        System.out.println("=== EntityManager bulk insert completed ===\n");
    }

    @Test
    @Transactional
    public void testJdbcTemplateBulkInsert() {
        System.out.println("\n=== Testing JdbcTemplate bulk insert ===");

        List<User> users = createUsersWithIds(25);
        bulkInsertService.bulkInsertWithJdbcTemplate(users);

        System.out.println("=== JdbcTemplate bulk insert completed ===\n");
    }

    @Test
    @Transactional
    public void testJdbcBatchInsert() throws SQLException {
        System.out.println("\n=== Testing JDBC batch insert ===");

        List<User> users = createUsersWithIds(25);
        bulkInsertService.bulkInsertWithJdbcBatch(users, 10);

        System.out.println("=== JDBC batch insert completed ===\n");
    }

    @Test
    @Transactional
    public void testNativeQueryBulkInsert() {
        System.out.println("\n=== Testing Native Query bulk insert ===");

        List<User> users = createUsersWithIds(25);
        bulkInsertService.bulkInsertWithNativeQuery(users);

        System.out.println("=== Native Query bulk insert completed ===\n");
    }

    private List<User> createUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            users.add(new User("User" + i, "user" + i + "@example.com"));
        }
        return users;
    }

    private List<User> createUsersWithIds(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            User user = new User("User" + i, "user" + i + "@example.com");
            user.setId((long) i);
            users.add(user);
        }
        return users;
    }
}