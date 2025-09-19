# JPA 배치 인서트 성능 분석 프로젝트 🚀

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> **JPA `saveAll()` 메서드에 대한 흔한 오해를 밝히고 진짜 벌크 인서트 구현 방법을 제시하는 종합 분석 프로젝트**

## 🎯 프로젝트 개요

이 프로젝트는 JPA의 `saveAll()` 메서드가 배치 인서트를 수행한다는 흔한 오해를 조사합니다. 체계적인 테스트와 성능 분석을 통해 `saveAll()`의 실제 동작을 보여주고, 진짜 벌크 인서트 작업의 여러 구현 방법을 제공합니다.

### 🔍 주요 발견사항

- ❌ **JPA `saveAll()`은 배치 인서트를 수행하지 않습니다** - 개별 INSERT 문을 실행합니다
- ⚙️ `hibernate.jdbc.batch_size` 설정은 `saveAll()` 작업에 **아무런 영향을 주지 않습니다**
- ✅ 진짜 벌크 인서트는 EntityManager, JdbcTemplate, 또는 네이티브 JDBC를 사용한 수동 구현이 필요합니다
- 🚀 **적절한 벌크 인서트 구현으로 최대 4.5배 성능 향상** 가능

## 🏗️ 프로젝트 구조

```
src/
├── main/java/com/example/batchsize/
│   ├── BatchSizeApplication.java      # Spring Boot 메인 클래스
│   ├── User.java                      # JPA 엔티티
│   ├── UserRepository.java           # JPA 레포지토리
│   └── BulkInsertService.java         # 벌크 인서트 구현체들
└── test/java/com/example/batchsize/
    ├── BatchInsertTest.java           # 기본 JPA saveAll() 테스트
    ├── BatchInsertWithoutBatchSizeTest.java  # 설정 비교 테스트
    ├── BulkInsertComparisonTest.java   # 방법별 비교 테스트
    ├── BulkInsertPerformanceTest.java  # 성능 벤치마크
    ├── PostgreSQLBatchInsertTest.java  # PostgreSQL 전용 테스트
    └── SimpleBulkInsertTest.java       # 간단한 성능 비교
```

## 🛠️ 기술 스택

- **프레임워크**: Spring Boot 3.1.5
- **ORM**: Hibernate 6.2.13
- **데이터베이스**: H2 (인메모리), PostgreSQL (Testcontainers 사용)
- **테스트**: JUnit 5, Spring Boot Test, Testcontainers
- **빌드 도구**: Gradle 8.12

## 🚀 시작하기

### 사전 요구사항

- Java 17 이상
- Docker (PostgreSQL 테스트용)

### 설치 및 실행

```bash
# 레포지토리 클론
git clone <repository-url>
cd batchsize

# 모든 테스트 실행
./gradlew test

# 특정 테스트 카테고리 실행
./gradlew test --tests "*BulkInsert*"
./gradlew test --tests "*Performance*"

# 상세한 SQL 로깅과 함께 실행
./gradlew test --tests "SimpleBulkInsertTest" -i
```

## 📊 벌크 인서트 구현 방법

이 프로젝트는 **4가지 다른 벌크 인서트 전략**을 제공합니다:

### 1. JPA Repository `saveAll()` (기준선)
```java
// ❌ 이것은 벌크 인서트가 아닙니다!
userRepository.saveAll(users);
// N개의 개별 INSERT 문을 실행합니다
```

### 2. EntityManager 배치 인서트
```java
// ✅ flush/clear 사이클을 사용한 진짜 배치 인서트
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
```

### 3. JdbcTemplate 배치 인서트
```java
// ✅ 가장 빠른 벌크 인서트 방법
@Transactional
public void bulkInsertWithJdbcTemplate(List<User> users) {
    String sql = "INSERT INTO users (name, email, id) VALUES (?, ?, ?)";
    jdbcTemplate.batchUpdate(sql, users, users.size(), (ps, user) -> {
        ps.setString(1, user.getName());
        ps.setString(2, user.getEmail());
        ps.setLong(3, user.getId());
    });
}
```

### 4. 네이티브 JDBC 배치 인서트
```java
// ✅ 배치 작업에 대한 최대 제어
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
```

## 📈 성능 벤치마크

### 테스트 결과 (1,000건 기준)

| 방법 | 실행 시간 | 성능 향상 |
|--------|---------------|------------------|
| JPA `saveAll()` | 99 ms | 기준선 |
| EntityManager 배치 | 63 ms | **1.6배 빠름** |
| JdbcTemplate 배치 | 22 ms | **4.5배 빠름** |
| JDBC 배치 | ~25 ms | **4.0배 빠름** |

### SQL 쿼리 분석

**JPA `saveAll()` 25건 처리:**
```sql
-- 25개의 개별 INSERT 문이 생성됨
INSERT INTO users (email,name,id) VALUES (?,?,?)  -- 레코드 1
INSERT INTO users (email,name,id) VALUES (?,?,?)  -- 레코드 2
-- ... 23개의 추가 개별 INSERT
```

**JdbcTemplate 배치 25건 처리:**
```sql
-- 단일 배치 작업 (로그에 개별 SQL 문이 보이지 않음)
-- JDBC 레벨에서 하나의 배치 작업으로 실행됨
```

## 🧪 테스트 카테고리

### 1. 기본 동작 테스트
- `BatchInsertTest`: JPA `saveAll()`의 개별 INSERT 동작을 보여줍니다
- `BatchInsertWithoutBatchSizeTest`: `batch_size` 설정이 효과가 없음을 보여줍니다

### 2. 방법 비교 테스트
- `BulkInsertComparisonTest`: 모든 방법의 나란한 비교
- `SimpleBulkInsertTest`: 빠른 성능 비교

### 3. 성능 분석
- `BulkInsertPerformanceTest`: 종합적인 성능 벤치마크
- `PostgreSQLBatchInsertTest`: Testcontainers를 사용한 데이터베이스별 테스트

## ⚙️ 설정

### Hibernate 배치 설정
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20              # ❌ saveAll()에는 효과 없음
        order_inserts: true           # ✅ EntityManager 배치에 도움
        order_updates: true           # ✅ EntityManager 배치에 도움
        batch_versioned_data: true    # ✅ 버전 관리 엔티티 배치 활성화
    show-sql: true                    # ✅ SQL 로깅 활성화
```

### 로깅 설정
```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.engine.jdbc.batch.internal.BatchingBatch: DEBUG
```

## 🔍 핵심 학습 내용

### `saveAll()` 오해
많은 개발자들이 JPA의 `saveAll()` 메서드가 배치 인서트를 수행한다고 믿습니다. **이는 잘못된 것입니다.**

- `saveAll()`은 내부적으로 각 엔티티에 대해 `save()`를 호출합니다
- 각 `save()`는 별도의 INSERT 문을 생성합니다
- `hibernate.jdbc.batch_size` 설정은 무시됩니다
- 성능은 레코드 수에 따라 선형적으로 확장됩니다 (O(n))

### 각 방법을 언제 사용할지

| 사용 사례 | 권장 방법 | 이유 |
|----------|-------------------|----------|
| 소규모 데이터셋 (<100건) | JPA `saveAll()` | 간단하고 읽기 쉬운 코드 |
| 중간 규모 데이터셋 (100-1000건) | EntityManager 배치 | JPA의 이점과 함께 좋은 성능 |
| 대규모 데이터셋 (>1000건) | JdbcTemplate 배치 | 최고의 성능 |
| 최대 제어가 필요한 경우 | JDBC 배치 | 배치 작업에 대한 완전한 제어 |

### 데이터베이스 호환성

| 데이터베이스 | JPA saveAll() | EntityManager | JdbcTemplate | JDBC 배치 |
|----------|---------------|---------------|--------------|------------|
| H2 | ✅ | ✅ | ✅ | ✅ |
| PostgreSQL | ✅ | ✅ | ✅ | ✅ |
| MySQL | ✅ | ✅ | ✅ | ✅ |
| Oracle | ✅ | ✅ | ✅ | ✅ |

## 🤝 기여하기

1. 레포지토리를 포크합니다
2. 피처 브랜치를 생성합니다 (`git checkout -b feature/AmazingFeature`)
3. 변경사항에 대한 테스트를 추가합니다
4. 변경사항을 커밋합니다 (`git commit -m 'Add some AmazingFeature'`)
5. 브랜치에 푸시합니다 (`git push origin feature/AmazingFeature`)
6. Pull Request를 열어주세요

## 📝 라이선스

이 프로젝트는 MIT 라이선스 하에 라이선스됩니다 - 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

## 🙏 감사의 말

- 훌륭한 문서를 제공하는 Spring Framework 팀
- 강력한 ORM 기능을 제공하는 Hibernate 팀
- 데이터베이스 통합 테스트를 가능하게 하는 Testcontainers 프로젝트

## 📚 참고 자료

- [Hibernate 배치 처리](https://docs.jboss.org/hibernate/orm/6.2/userguide/html_single/Hibernate_User_Guide.html#batch)
- [Spring Data JPA 레퍼런스](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [JDBC 배치 처리](https://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html#batch_updates)

---

> **💡 팁**: 항상 본인의 특정 사용 사례에서 성능을 측정하세요. 데이터베이스 설정, 엔티티 복잡성, 인프라가 결과에 상당한 영향을 미칠 수 있습니다.