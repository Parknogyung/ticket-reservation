# 🎟️ High-Traffic Concert Ticketing System (대규모 트래픽 티켓팅 서비스)

> **"1,000명이 동시에 0.1초 만에 접속해도, 결코 오버부킹(Overbooking)은 발생하지 않습니다."**

이 프로젝트는 인기 콘서트 예매 상황과 같은 **대용량 트래픽 환경**을 시뮬레이션하고, 이를 기술적으로 해결하기 위해 설계된 **분산 처리 티켓팅 시스템**입니다. 개인 포트폴리오 용도로 개발되었으며, **동시성 제어(Concurrency Control)**와 **시스템 성능 최적화**에 중점을 두었습니다.

---

## 🛠️ Tech Stack

### Backend
- **Language**: Java 21 (Virtual Threads 적용)
- **Framework**: Spring Boot 3.3.5
- **Communication**: gRPC (Protobuf)
- **Database**: MariaDB, Redis
- **ORM**: Spring Data JPA, QueryDSL
- **Concurrency**: Redisson (Distributed Lock), Atomic Operations

### Infrastructure & Tools
- **Build Tool**: Gradle
- **Containerization**: Docker, Docker Compose
- **Security**: Spring Security, JWT

---

## 🏗️ Architecture & Key Features

### 1. gRPC 기반의 고성능 통신
REST API 대신 **gRPC**를 도입하여 서비스 간 통신 속도를 극대화하고 데이터 전송 크기를 줄였습니다. Protocol Buffers를 사용하여 직렬화/역직렬화 성능을 높였습니다.

### 2. Java 21 Virtual Threads 도입
기존의 플랫폼 스레드(Platform Thread) 모델 대신 **Virtual Threads**를 사용하여 블로킹 I/O가 많은 티켓팅 요청을 효율적으로 처리합니다. 이를 통해 제한된 리소스로도 높은 처리량(Throughput)을 달성했습니다.

### 3. 동시성 제어 및 데이터 무결성 (No Overbooking)
티켓 예매의 핵심인 **중복 예약 방지**를 위해 다층적인 동시성 제어 전략을 사용했습니다.
- **Redis (Redisson)**: 분산 락(Distributed Lock)을 적용하여 다중 서버 환경에서도 좌석 선점의 원자성(Atomicity)을 보장합니다.
- **Database Lock**: 낙관적 락(Optimistic Lock) 등을 고려하여 데이터 정합성을 유지합니다.

### 4. MSA 지향적 모듈 설계
프로젝트는 기능별로 모듈화되어 있어 확장성을 고려했습니다.
- **`server`**: 핵심 비즈니스 로직 (예약, 좌석 관리, 콘서트 정보).
- **`auth`**: 사용자 인증 및 인가 (JWT 발급).
- **`client`**: gRPC 클라이언트 및 부하 테스트용 모듈.
- **`proto`**: gRPC 통신을 위한 IDL 정의 및 공유 모듈.

---

## 📂 Project Structure

```
ticket
├── auth        # 인증 서비스 (JWT, Spring Security)
├── client      # gRPC 클라이언트 & 로드 제너레이터
├── proto       # Protocol Buffers (.proto) 정의
├── server      # 메인 티켓팅 서버 (Business Logic)
│   ├── config      # Redis, gRPC 설정
│   ├── controller  # gRPC Service 구현
│   ├── domain      # Entity 정의
│   ├── repository  # JPA & QueryDSL Repository
│   └── service     # 트랜잭션 처리 및 비즈니스 로직
└── compose.yaml    # Docker Compose 설정
```

---

## 🚀 Getting Started

### Prerequisites
- JDK 21
- Docker & Docker Compose

### Build & Run
1. **프로젝트 빌드**
   ```bash
   ./gradlew clean build
   ```

2. **서비스 실행 (Docker Compose)**
   ```bash
   docker-compose up -d
   ```

3. **개별 모듈 실행**
   - Server: `./gradlew :server:bootRun`
   - Auth: `./gradlew :auth:bootRun`

---

## 📝 Recent Updates & Implementation Details
- **RedisConfig**: Redisson 클라이언트 설정 및 캐시 전략 구성.
- **Repositories**: `Concert`, `Seat`, `Reservation` 등 핵심 도메인의 데이터 접근 계층 구현.
- **Concurrency**: `AtomicInteger` 및 Redis 락을 활용한 좌석 차감 로직 고도화.
- **Blacklist**: 악성 사용자 차단을 위한 블랙리스트 관리 기능 추가.
