핵심 기술 스택 (Tech Stack)
Language: Java 21 (LTS) - Virtual Threads 활용

Framework: Spring Boot 3.3

Communication: gRPC, Protobuf

Database: MariaDB (Docker)

Cache & Lock: Redis, Redisson (Docker)

Testing: JUnit 5, Mockito

Build Tool: Gradle

⚡ Key Features & Challenges
이 프로젝트의 핵심은 **"동시성 제어(Concurrency Control)"**와 **"데이터 무결성"**입니다.

1. 분산 락(Distributed Lock)을 통한 오버부킹 방지
문제(Challenge): 1,000명의 유저가 동시에 하나의 좌석(Seat ID: 1)을 예약하려고 할 때, 전통적인 DB 락(Pessimistic Lock)은 성능 저하를 일으키고, 낙관적 락(Optimistic Lock)은 잦은 실패로 사용자 경험을 해칩니다.

해결(Solution): **Redis(Redisson)**을 활용한 분산 락을 도입했습니다.

Pub/Sub 방식을 사용하여 스핀 락(Spin Lock)으로 인한 Redis 부하를 최소화했습니다.

트랜잭션 커밋 시점과 락 해제 시점의 불일치(Race Condition)를 해결하기 위해 **TransactionTemplate**을 사용하여 락 범위 내에서 트랜잭션을 완벽하게 처리했습니다.

2. Java 21 Virtual Threads 도입
문제: 기존의 플랫폼 스레드(Platform Thread) 모델은 1,000명 이상의 동시 접속 처리 시 컨텍스트 스위칭 비용이 높고 메모리 사용량이 많았습니다.

해결: JDK 21의 **가상 스레드(Virtual Threads)**를 ExecutorService에 적용하여, OS 스레드를 차단하지 않고 가볍게 수천 개의 요청을 병렬 처리하는 부하 테스트 환경을 구축했습니다.

3. gRPC 통신 최적화
HTTP/1.1 기반의 REST API 대신, HTTP/2 기반의 gRPC를 채택하여 헤더 압축과 바이너리 전송을 통해 네트워크 오버헤드를 줄였습니다. 이는 향후 MSA 환경에서 서비스 간 통신 효율을 높이는 기반이 됩니다.

📊 Performance Test Result
자체 개발한 **부하 테스트 클라이언트(Client Module)**를 통해 검증을 수행했습니다.

시나리오: 준비된 좌석 50개, 동시 접속자 1,000명 (경쟁률 20:1)

검증 목표: 정확히 50명만 예약에 성공하고, 950명은 실패해야 함. 또한 데이터베이스에 중복 예약(Overbooking)이 단 1건도 없어야 함.

테스트 결과 로그
Plaintext

========== [🔥 부하 테스트 시작] 유저 1000명 동시 접속 시도 ==========
...
========== [결과 리포트] ==========
총 소요 시간: 1135ms
총 시도 횟수: 1000
✅ 예약 성공: 50
❌ 예약 실패: 950
================================
👍 검증 성공: 동시성 제어가 완벽하게 작동했습니다.
🚀 How to Run
1. 인프라 실행 (Docker)
MariaDB와 Redis 컨테이너를 실행합니다.

Bash

docker-compose up -d
2. 서버 실행 (Server Module)
TicketServerApplication.java를 실행합니다. 서버 시작 시 DataInitializer가 자동으로 테스트용 데이터(유저 1,000명, 좌석 50개)를 생성하고 Redis를 초기화합니다.

3. 클라이언트 실행 (Client Module)
TicketClientApplication.java를 실행하면 자동으로 부하 테스트 시나리오가 시작됩니다.

🔮 Future Roadmap (확장 계획)
현재는 백엔드 코어 로직과 부하 테스트에 집중되어 있으며, 향후 다음과 같이 확장하여 완전한 웹 서비스로 발전시킬 계획입니다.

[Frontend] React/Next.js를 도입하여 실제 예매 가능한 웹 페이지 구현

[API Gateway] gRPC-Web 또는 Envoy Proxy를 통해 브라우저(HTTP)와 gRPC 서버 간 통신 연결

[Queue System] 대기열 순번을 시각적으로 보여주는 WebSocket 기능 추가

[Payment] PG사 연동(Portone 등)을 통한 결제 모듈 탑재

👨‍💻 Developer
Name: [본인 이름/닉네임]

Contact: [이메일 주소]
