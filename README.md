# 🎟️ High-Traffic Concert Ticketing System (대규모 트래픽 티켓팅 서비스)

> **"1,000명이 동시에 0.1초 만에 접속해도, 결코 오버부킹(Overbooking)은 발생하지 않습니다."**

이 프로젝트는 인기 콘서트 예매 상황과 같은 **대용량 트래픽 환경**을 시뮬레이션하고, 이를 기술적으로 해결하기 위해 설계된 **분산 처리 티켓팅 시스템**입니다.

현재는 **gRPC 기반의 고성능 서버**와 **부하 테스트 클라이언트**로 구성되어 있으며, 추후 웹 프론트엔드 확장을 고려하여 **MSA(Microservice Architecture) 지향적**으로 설계되었습니다.

---

## 🏗️ Architecture & Flow

### 시스템 아키텍처
REST API 대신 **gRPC**를 사용하여 데이터 전송 크기를 줄이고 통신 속도를 극대화했습니다. 또한, **Java 21 Virtual Threads**를 도입하여 블로킹 I/O 상황에서도 스레드 자원을 효율적으로 사용하여 높은 처리량(Throughput)을 보장합니다.

```mermaid
graph TD
    Client[Client / Load Generator] -- gRPC (Protobuf) --> Server[Ticket Server]
    
    subgraph Infrastructure
        Server -- "Virtual Threads" --> CoreLogic{Business Logic}
        CoreLogic -- "Distributed Lock (Redisson)" --> Redis[(Redis Cache)]
        CoreLogic -- "JPA / Hibernate" --> DB[(MariaDB)]
    end
    
    subgraph Flow
        Redis -.-> |"1. 대기열 검증 (Queue)"| CoreLogic
        Redis -.-> |"2. 좌석 선점 (Lock)"| CoreLogic
        DB -.-> |"3. 최종 결제/예약 (Persist)"| CoreLogic
    end
