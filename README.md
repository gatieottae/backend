# 같이어때 Backend

## 🚀 실행 환경
- Java 17
- Spring Boot 3.5.5
- Gradle
- PostgreSQL (로컬 개발 DB)
- H2 (테스트용 인메모리 DB)

## ⚙️ 기본 세팅
- Gradle 초기 설정
- Spring Boot 기본 구조
- `.gitignore` 추가
- `application-example.yml` 템플릿 추가
- PostgreSQL 의존성 및 연결 설정
- JPA/Hibernate 기본 설정

## 📂 주요 디렉토리
- `domain/` : 엔티티 및 도메인 클래스
- `repository/` : JPA Repository 인터페이스
- `service/` : 비즈니스 로직
- `api/` : Controller(API) 계층
- `config/` : 공통 설정
- `common/` : 공용 유틸/추상 클래스 (예: `BaseTimeEntity`)

## 🧪 테스트
### MemberRepositoryTest
프로젝트의 JPA 및 Repository 기본 동작을 보장하기 위해 작성한 테스트입니다.

- **검증 대상**
    - `Member` 엔티티가 JPA 매핑을 통해 정상적으로 DB 테이블과 연결되는지
    - `MemberRepository`의 기본 메서드(`save`, `findByEmail`)가 정상 동작하는지

- **테스트 방식**
    - H2 인메모리 DB를 사용하여 매 실행마다 깨끗한 DB 환경에서 테스트가 진행됩니다.
    - `@DataJpaTest`를 통해 JPA 관련 컴포넌트만 로딩하여 빠르고 독립적인 테스트가 가능합니다.

- **의미**
    - 엔티티/레포지토리 기본 구조와 매핑 설정이 올바른지 보장
    - DB 스키마 설정 오류, 쿼리 메서드 이름 오류 등을 조기 발견
    - 협업 시 다른 개발자가 `./gradlew test` 실행만으로도 JPA 기본 동작 검증 가능
    - 코드 변경 후에도 기존 Repository 동작에 영향이 없는지 안전망 제공