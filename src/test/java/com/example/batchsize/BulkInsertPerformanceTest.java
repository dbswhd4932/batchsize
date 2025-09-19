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
    "spring.jpa.properties.hibernate.jdbc.batch_size=100",
    "spring.jpa.properties.hibernate.order_inserts=true",
    "spring.jpa.properties.hibernate.order_updates=true",
    "spring.jpa.properties.hibernate.batch_versioned_data=true",
    "spring.jpa.show-sql=false", // SQL 로그 끄기 (성능 측정을 위해)
    "logging.level.org.hibernate.SQL=WARN"
})
public class BulkInsertPerformanceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BulkInsertService bulkInsertService;

    private static final int RECORD_COUNT = 1000;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    public void compareInsertPerformance() throws SQLException {
        System.out.println("=== Bulk Insert Performance Comparison ===");
        System.out.println("Record count: " + RECORD_COUNT);
        System.out.println();

        // 1. JPA saveAll()
        measurePerformance("JPA saveAll()", () -> {
            List<User> users = createUsers(RECORD_COUNT);
            userRepository.saveAll(users);
            userRepository.deleteAll();
        });

        // 2. EntityManager bulk insert
        measurePerformance("EntityManager Bulk Insert", () -> {
            List<User> users = createUsers(RECORD_COUNT);
            bulkInsertService.bulkInsertWithEntityManager(users, 100);
            userRepository.deleteAll();
        });

        // 3. JdbcTemplate batch
        measurePerformance("JdbcTemplate Batch", () -> {
            List<User> users = createUsersWithIds(RECORD_COUNT);
            bulkInsertService.bulkInsertWithJdbcTemplate(users);
            userRepository.deleteAll();
        });

        // 4. JDBC batch
        measurePerformance("JDBC Batch", () -> {
            List<User> users = createUsersWithIds(RECORD_COUNT);
            try {
                bulkInsertService.bulkInsertWithJdbcBatch(users, 100);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            userRepository.deleteAll();
        });

        // 5. Native Query bulk insert
        measurePerformance("Native Query Bulk Insert", () -> {
            List<User> users = createUsersWithIds(RECORD_COUNT);
            bulkInsertService.bulkInsertWithNativeQuery(users);
            userRepository.deleteAll();
        });
    }

    @Test
    @Transactional
    public void testDifferentBatchSizes() {
        System.out.println("=== Testing Different Batch Sizes ===");
        System.out.println();

        int[] batchSizes = {10, 50, 100, 200, 500};
        List<User> users = createUsersWithIds(1000);

        for (int batchSize : batchSizes) {
            measurePerformance("EntityManager Batch Size " + batchSize, () -> {
                List<User> testUsers = createUsers(1000);
                bulkInsertService.bulkInsertWithEntityManager(testUsers, batchSize);
                userRepository.deleteAll();
            });
        }
    }

    private void measurePerformance(String testName, Runnable test) {
        System.gc(); // GC 실행으로 메모리 정리

        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemory();

        test.run();

        long endTime = System.currentTimeMillis();
        long endMemory = getUsedMemory();

        long executionTime = endTime - startTime;
        long memoryUsed = endMemory - startMemory;

        System.out.printf("%-30s: %5d ms, Memory: %+6d KB%n",
            testName, executionTime, memoryUsed / 1024);
    }

    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
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