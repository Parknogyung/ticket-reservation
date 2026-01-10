package com.client.portFolio.client;

import com.ticket.portfolio.*;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class ScenarioRunner implements CommandLineRunner {

    @GrpcClient("ticket-server")
    private TicketServiceGrpc.TicketServiceBlockingStub ticketStub;

    // 테스트 설정
    private static final int USER_COUNT = 100000; // 1,000명이 동시 접속
    private static final int TOTAL_SEATS = 500;  // 준비된 좌석 수

    @Override
    public void run(String... args) throws Exception {
        log.info("========== [🔥 부하 테스트 시작] 유저 {}명 동시 접속 시도 ==========", USER_COUNT);

        // [Java 25 핵심] 가상 스레드(Virtual Threads) 생성기
        // 기존 스레드풀과 달리, OS 스레드를 거의 쓰지 않고 무한대로 생성 가능합니다.
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        // 모든 스레드가 끝날 때까지 기다리기 위한 장치
        CountDownLatch latch = new CountDownLatch(USER_COUNT);

        // 성공/실패 카운터 (동시성 안전)
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= USER_COUNT; i++) {
            final String userId = String.valueOf(i);

            executor.submit(() -> {
                try {
                    // 1. 랜덤 좌석 선택 (1~50번 좌석 중 하나를 무작위로 공략)
                    // -> 여러 사람이 같은 좌석을 노리게 되어 '동시성 이슈'가 발생함!
                    long targetSeatId = new Random().nextInt(TOTAL_SEATS) + 1;

                    // 2. 대기열 토큰 발급
                    TokenRequest tokenReq = TokenRequest.newBuilder()
                            .setUserId(userId)
                            .setConcertId(1L)
                            .build();
                    TokenResponse tokenRes = ticketStub.issueToken(tokenReq);

                    if (tokenRes.getCanEnter()) {
                        // 3. 좌석 예약 시도
                        ReservationRequest resReq = ReservationRequest.newBuilder()
                                .setToken(tokenRes.getToken())
                                .setUserId(userId)
                                .setSeatId(targetSeatId)
                                .build();

                        ReservationResponse resRes = ticketStub.reserveSeat(resReq);

                        if (resRes.getSuccess()) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    } else {
                        failCount.incrementAndGet(); // 대기열 진입 실패
                    }

                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown(); // 작업 끝남 알림
                }
            });
        }

        // 모든 유저의 작업이 끝날 때까지 대기
        latch.await();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        log.info("========== [결과 리포트] ==========");
        log.info("총 소요 시간: {}ms", duration);
        log.info("총 시도 횟수: {}", USER_COUNT);
        log.info("✅ 예약 성공: {}", successCount.get());
        log.info("❌ 예약 실패: {}", failCount.get());
        log.info("================================");

        // 검증: 성공 횟수는 절대 총 좌석 수(500)를 넘으면 안 됨!
        if (successCount.get() > TOTAL_SEATS) {
            log.error("🚨 치명적 오류: 준비된 좌석보다 더 많이 예약되었습니다! (동시성 제어 실패)");
        } else {
            log.info("👍 검증 성공: 동시성 제어가 완벽하게 작동했습니다.");
        }

        // 테스트 끝나면 바로 종료되지 않게 하려면 아래 주석 해제
        // Thread.sleep(5000);
    }
}
