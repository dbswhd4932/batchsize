# Batch Size 테스트 프로젝트

JPA `saveAll()` 메서드의 배치 인서트 동작을 테스트하는 프로젝트입니다.

## 테스트 목적
- `saveAll()` 메서드 사용 시 여러 번의 INSERT 쿼리가 발생하는지 확인
- `batch_size` 설정 시 벌크 인서트가 되는지 확인

## 실행 방법

```bash
cd batchsize
./gradlew test
```

## 설정 파일

### application.yml
- `batch_size: 10` 설정으로 배치 인서트 활성화
- SQL 로깅 활성화

### 테스트 케이스

1. **testSaveAllWithBatchSize**: batch_size=10으로 25개 데이터 저장
2. **testSaveAllWithoutBatchSize**: batch_size=1로 10개 데이터 저장

## 예상 결과

- batch_size=10: 25개 데이터를 3번의 배치 인서트로 처리 (10 + 10 + 5)
- batch_size=1: 10개 데이터를 10번의 개별 인서트로 처리