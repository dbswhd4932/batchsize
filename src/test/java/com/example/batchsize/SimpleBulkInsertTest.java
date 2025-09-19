package com.example.batchsize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class SimpleBulkInsertTest {

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
    public void quickPerformanceComparison() {
        System.out.println("=== Quick Bulk Insert Performance Test ===");

        int recordCount = 100;

        // 1. JPA saveAll()
        long start = System.currentTimeMillis();
        List<User> users1 = createUsers(recordCount);
        userRepository.saveAll(users1);
        long jpaTime = System.currentTimeMillis() - start;
        userRepository.deleteAll();

        // 2. JdbcTemplate batch
        start = System.currentTimeMillis();
        List<User> users2 = createUsersWithIds(recordCount);
        bulkInsertService.bulkInsertWithJdbcTemplate(users2);
        long jdbcTime = System.currentTimeMillis() - start;
        userRepository.deleteAll();

        // 3. EntityManager bulk insert
        start = System.currentTimeMillis();
        List<User> users3 = createUsers(recordCount);
        bulkInsertService.bulkInsertWithEntityManager(users3, 20);
        long entityManagerTime = System.currentTimeMillis() - start;

        System.out.printf("Record count: %d%n", recordCount);
        System.out.printf("JPA saveAll():           %3d ms%n", jpaTime);
        System.out.printf("JdbcTemplate batch:      %3d ms%n", jdbcTime);
        System.out.printf("EntityManager batch:     %3d ms%n", entityManagerTime);
        System.out.println();

        if (jdbcTime < jpaTime) {
            System.out.printf("JdbcTemplate is %.1fx faster than JPA saveAll()%n",
                (double) jpaTime / jdbcTime);
        }
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