package com.server.portfolio.service;

import com.server.portfolio.domain.Reservation;
import com.server.portfolio.domain.Seat;
import com.server.portfolio.domain.User;
import com.server.portfolio.repository.ReservationRepository;
import com.server.portfolio.repository.SeatRepository;
import com.server.portfolio.repository.UserRepository;
import com.ticket.portfolio.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class TicketGrpcService extends TicketServiceGrpc.TicketServiceImplBase {
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    private final RedisTemplate<String, String> redisTemplate;
    private final RedissonClient redissonClient;
    private final PlatformTransactionManager transactionManager;

    // 1. 대기열 토큰 발급
    @Override
    public void issueToken(TokenRequest request, StreamObserver<TokenResponse> responseObserver) {
        String userId = request.getUserId();
        String queueKey = "concert_queue:" + request.getConcertId();

        // Redis Sorted Set에 추가 (Score = 현재 시간)
        // 먼저 온 사람이 적은 값을 가짐 -> 먼저 나감
        long timestamp = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(queueKey, userId, timestamp);

        // 내 순서 확인 (Rank)
        Long rank = redisTemplate.opsForZSet().rank(queueKey, userId);

        TokenResponse response = TokenResponse.newBuilder()
                .setToken(UUID.randomUUID().toString()) // 실제로는 JWT 등을 사용
                .setWaitPosition(rank != null ? rank : -1)
                .setCanEnter(rank != null && rank < 2000) // 예: 상위 100명만 입장 가능
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // 2. 좌석 예약 (핵심: 분산 락 적용)
    @Override
    public void reserveSeat(ReservationRequest request, StreamObserver<ReservationResponse> responseObserver) {
        long seatId = request.getSeatId();
        long userId = Long.parseLong(request.getUserId());
        String lockKey = "seat_lock:" + seatId;

        // Redisson 분산 락 획득
        RLock lock = redissonClient.getLock(lockKey);

        // 람다 내부에서 값을 변경하기 위해 Atomic 변수 사용
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicReference<String> message = new AtomicReference<>("");
        AtomicLong reservationId = new AtomicLong(-1);

        try {
            // 락 시도 (최대 10초 대기, 락 획득 후 10초 뒤 자동 해제)
            boolean available = lock.tryLock(10, 10, TimeUnit.SECONDS);

            if (!available) {
                log.warn("락 획득 실패: seatId={}, userId={}", seatId, userId);
                throw new RuntimeException("접속자가 많아 처리가 지연되고 있습니다.");
            }

            // --- [임계 구역: 오직 한 명만 들어옴] ---
            // 트랜잭션 범위를 락 내부로 제한하여, 락 해제 전에 커밋이 완료되도록 보장함
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                Seat seat = seatRepository.findById(seatId)
                        .orElseThrow(() -> new IllegalArgumentException("좌석이 존재하지 않습니다."));

                if (seat.getStatus() != Seat.SeatStatus.AVAILABLE) {
                    message.set("이미 예약된 좌석입니다.");
                    return;
                }

                // 예약 진행
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다."));

                // 상태 변경 (Dirty Checking)
                seat.reserve();

                // 예약 내역 생성
                Reservation reservation = Reservation.builder()
                        .user(user)
                        .seat(seat)
                        .reservationTime(LocalDateTime.now())
                        .status(Reservation.ReservationStatus.PENDING)
                        .build();

                reservationRepository.save(reservation);

                success.set(true);
                message.set("예약에 성공했습니다.");
                reservationId.set(reservation.getId());
            });
            // ------------------------------------

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            message.set("서버 에러가 발생했습니다.");
        } catch (Exception e) {
            log.error("예약 실패", e);
            message.set(e.getMessage());
        } finally {
            // 락 해제 (중요: 내가 건 락만 해제)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        ReservationResponse response = ReservationResponse.newBuilder()
                .setSuccess(success.get())
                .setMessage(message.get())
                .setReservationId(reservationId.get())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
