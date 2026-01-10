package com.server.portfolio.config;

import com.server.portfolio.domain.Concert;
import com.server.portfolio.domain.ConcertOption;
import com.server.portfolio.domain.Seat;
import com.server.portfolio.domain.User;
import com.server.portfolio.repository.ConcertOptionRepository;
import com.server.portfolio.repository.ConcertRepository;
import com.server.portfolio.repository.SeatRepository;
import com.server.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private final ConcertRepository concertRepository;
    private final ConcertOptionRepository concertOptionRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // [핵심] 서버 시작 시 Redis 대기열 초기화 (이전 테스트 데이터 삭제)
        // 이게 없으면 이전 100만 명 데이터 때문에 신규 유저가 대기열 끝으로 밀려나 입장을 못함
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
        log.info("🧹 Redis 대기열 데이터 완전 초기화 완료!");

        // 1. 유저 생성
        if (userRepository.count() == 0) {
            int totalUsers = 1000000;
            int batchSize = 10000;

            log.info("🚀 유저 데이터 생성 시작 (JDBC Batch, 총 {}명)...", totalUsers);
            // 주의: 테이블 이름이 'users'라고 가정했습니다. (Entity가 @Table(name="users")이거나 기본 설정)
            String sql = "INSERT INTO user (email, password, role, point) VALUES (?, ?, ?, ?)";

            for (int i = 0; i < totalUsers; i += batchSize) {
                List<Object[]> batchArgs = new ArrayList<>();
                for (int j = 0; j < batchSize; j++) {
                    int userIndex = i + j + 1;
                    batchArgs.add(new Object[]{
                            "user" + userIndex + "@test.com",
                            "1234",
                            User.Role.USER.name(), // Enum을 String으로 저장
                            10000L
                    });
                }
                jdbcTemplate.batchUpdate(sql, batchArgs);
                log.info("... {}명 저장 완료", i + batchSize);
            }
            log.info("✅ 테스트용 유저 {}명 생성 완료!", totalUsers);
        }

        // 2. 공연 및 좌석 생성
        if (concertRepository.count() == 0) {
            Concert concert = new Concert("아이유 콘서트 2026");
            concertRepository.save(concert);

            ConcertOption option = new ConcertOption(concert, LocalDateTime.now().plusDays(30));
            concertOptionRepository.save(option);

            // 좌석 500개 생성
            List<Seat> seats = new ArrayList<>();
            for (int i = 1; i <= 500; i++) {
                Seat seat = new Seat(option, i, Seat.SeatStatus.AVAILABLE);
                seats.add(seat);
            }
            seatRepository.saveAll(seats);
            log.info("✅ 테스트용 좌석 {}개 생성 완료!", seats.size());
        }
    }
}
