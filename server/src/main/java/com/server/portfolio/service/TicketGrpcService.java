package com.server.portfolio.service;

import com.server.portfolio.domain.*;
import com.server.portfolio.repository.*;
import com.server.portfolio.security.JwtTokenProvider;
import com.ticket.portfolio.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    private final ConcertRepository concertRepository;
    private final ConcertOptionRepository concertOptionRepository;

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final PlatformTransactionManager transactionManager;
    private final JwtTokenProvider jwtTokenProvider;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ① 티켓(콘서트) 등록 (Admin용)
    @Override
    public void registerConcert(RegisterConcertRequest request,
            StreamObserver<RegisterConcertResponse> responseObserver) {
        String title = request.getTitle();
        int seatCount = request.getSeatCount();
        String dateStr = request.getConcertDate();

        log.info("Registering concert: {} with {} seats", title, seatCount);

        try {
            AtomicLong concertOptionId = new AtomicLong(-1);
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                // 1. 공연 생성
                Concert concert = new Concert(title);
                concertRepository.save(concert);

                // 2. 공연 옵션(회차) 생성
                LocalDateTime concertDate = LocalDateTime.parse(dateStr, DATE_FORMATTER);
                ConcertOption option = new ConcertOption(concert, concertDate);
                concertOptionRepository.save(option);
                concertOptionId.set(option.getId());

                // 3. 좌석 생성
                java.util.List<com.server.portfolio.domain.Seat> seats = new java.util.ArrayList<>();
                for (int i = 1; i <= seatCount; i++) {
                    seats.add(com.server.portfolio.domain.Seat.builder()
                            .concertOption(option)
                            .seatNumber(i)
                            .status(com.server.portfolio.domain.Seat.SeatStatus.AVAILABLE)
                            .build());
                }
                seatRepository.saveAll(seats);
            });

            responseObserver.onNext(RegisterConcertResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("성공적으로 등록되었습니다.")
                    .setConcertId(concertOptionId.get())
                    .build());
        } catch (Exception e) {
            log.error("Concert registration failed", e);
            responseObserver.onNext(RegisterConcertResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("등록 실패: " + e.getMessage())
                    .build());
        }
        responseObserver.onCompleted();
    }

    // ② 티켓(콘서트) 목록 조회
    @Override
    @Transactional(readOnly = true)
    public void getConcerts(GetConcertsRequest request, StreamObserver<ConcertListResponse> responseObserver) {
        log.info("Fetching all concerts");
        try {
            List<ConcertOption> options = concertOptionRepository.findAll();
            ConcertListResponse.Builder responseBuilder = ConcertListResponse.newBuilder();

            for (ConcertOption option : options) {
                // 해당 회차의 남은 좌석 수 계산 (간단히 DB 카운트)
                // 실제 고사양 환경에선 Redis 캐시 권장
                long availableCount = seatRepository.findAvailableSeats(option.getId()).size();

                responseBuilder.addConcerts(ConcertInfo.newBuilder()
                        .setConcertId(option.getId())
                        .setTitle(option.getConcert().getTitle())
                        .setConcertDate(option.getConcertDate().format(DATE_FORMATTER))
                        .setAvailableSeats((int) availableCount)
                        .build());
            }
            responseObserver.onNext(responseBuilder.build());
        } catch (Exception e) {
            log.error("Failed to fetch concerts", e);
        }
        responseObserver.onCompleted();
    }

    // ③ 예약 가능한 좌석 조회
    @Override
    @Transactional(readOnly = true)
    public void getAvailableSeats(SeatSearchRequest request, StreamObserver<SeatListResponse> responseObserver) {
        long concertId = request.getConcertId();
        log.info("Fetching available seats for concert option: {}", concertId);

        try {
            List<com.server.portfolio.domain.Seat> seats = seatRepository.findAvailableSeats(concertId);

            SeatListResponse.Builder responseBuilder = SeatListResponse.newBuilder();
            for (com.server.portfolio.domain.Seat seat : seats) {
                responseBuilder.addSeats(com.ticket.portfolio.Seat.newBuilder()
                        .setSeatId(seat.getId())
                        .setSeatNumber(seat.getSeatNumber())
                        .setStatus(seat.getStatus().name())
                        .build());
            }
            responseObserver.onNext(responseBuilder.build());
        } catch (Exception e) {
            log.error("Failed to fetch seats", e);
        }
        responseObserver.onCompleted();
    }

    // ④ 좌석 예약 요청 (분산 락 적용)
    @Override
    public void reserveSeat(ReservationRequest request, StreamObserver<ReservationResponse> responseObserver) {
        long seatId = request.getSeatId();
        long userId = Long.parseLong(request.getUserId());
        String lockKey = "seat_lock:" + seatId;

        RLock lock = redissonClient.getLock(lockKey);

        AtomicBoolean success = new AtomicBoolean(false);
        AtomicReference<String> message = new AtomicReference<>("");
        AtomicLong reservationId = new AtomicLong(-1);

        try {
            boolean available = lock.tryLock(10, 10, TimeUnit.SECONDS);

            if (!available) {
                log.warn("락 획득 실패: seatId={}, userId={}", seatId, userId);
                throw new RuntimeException("접속자가 많아 처리가 지연되고 있습니다.");
            }

            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                com.server.portfolio.domain.Seat seat = seatRepository.findById(seatId)
                        .orElseThrow(() -> new IllegalArgumentException("좌석이 존재하지 않습니다."));

                if (seat.getStatus() != com.server.portfolio.domain.Seat.SeatStatus.AVAILABLE) {
                    message.set("이미 예약된 좌석입니다.");
                    return;
                }

                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다."));

                seat.reserve();

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

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            message.set("서버 에러가 발생했습니다.");
        } catch (Exception e) {
            log.error("예약 실패", e);
            message.set(e.getMessage());
        } finally {
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

    // ⑤ 대기열 토큰 발급
    @Override
    public void issueToken(TokenRequest request, StreamObserver<TokenResponse> responseObserver) {
        String userId = request.getUserId();
        String queueKey = "concert_queue:" + request.getConcertId();

        long timestamp = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(queueKey, userId, timestamp);

        Long rank = redisTemplate.opsForZSet().rank(queueKey, userId);

        TokenResponse response = TokenResponse.newBuilder()
                .setToken(UUID.randomUUID().toString())
                .setWaitPosition(rank != null ? rank : -1)
                .setCanEnter(rank != null && rank < 2000)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // ⑥ 로그인
    @Override
    public void login(com.ticket.portfolio.LoginRequest request,
            StreamObserver<com.ticket.portfolio.LoginResponse> responseObserver) {
        String email = request.getEmail();
        String password = request.getPassword();

        log.info("gRPC login request for email: {}", email);

        com.ticket.portfolio.LoginResponse.Builder responseBuilder = com.ticket.portfolio.LoginResponse.newBuilder();

        try {
            if ("user1@test.com".equals(email) && "1234".equals(password)) {
                log.info("Demo user login success: {}", email);
                // 데모 유저도 DB에서 조회해 ID를 가져오거나 없으면 생성
                User user = userRepository.findByEmail(email).orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .password(password)
                            .role(User.Role.USER)
                            .point(0L)
                            .build();
                    return userRepository.save(newUser);
                });
                sendLoginSuccess(user, responseBuilder, responseObserver);
                return;
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

            if (user.getPassword().equals(password)) {
                log.info("DB user login success: {}", email);
                sendLoginSuccess(user, responseBuilder, responseObserver);
            } else {
                log.warn("Login failed: password mismatch for {}", email);
                responseBuilder.setSuccess(false).setMessage("비밀번호가 일치하지 않습니다.");
                responseObserver.onNext(responseBuilder.build());
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            log.error("Login process error: ", e);
            responseBuilder.setSuccess(false).setMessage("인증 과정에서 오류가 발생했습니다: " + e.getMessage());
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        }
    }

    private void sendLoginSuccess(User user, com.ticket.portfolio.LoginResponse.Builder responseBuilder,
            StreamObserver<com.ticket.portfolio.LoginResponse> responseObserver) {
        String email = user.getEmail();
        org.springframework.security.core.Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                email, null, java.util.Collections.singletonList(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));

        String accessToken = jwtTokenProvider.createAccessToken(auth);
        String refreshToken = jwtTokenProvider.createRefreshToken(auth);

        redisTemplate.opsForValue().set("RT:" + email, refreshToken, 7, TimeUnit.DAYS);

        com.ticket.portfolio.LoginResponse response = responseBuilder.setSuccess(true)
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken)
                .setUserId(user.getId())
                .setMessage("로그인에 성공했습니다.")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
