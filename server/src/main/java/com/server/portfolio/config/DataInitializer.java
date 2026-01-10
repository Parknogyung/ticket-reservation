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

    @Override
    public void run(String... args) throws Exception {
        // 1. 유저 생성
        if (userRepository.count() == 0) {
            List<User> users = new ArrayList<>();
            for (int i = 1; i <= 1000; i++) {
                User user = User.builder()
                        .email("user" + i + "@test.com")
                        .password("1234")
                        .role(User.Role.USER)
                        .point(10000L)
                        .build();
                users.add(user);
            }
            userRepository.saveAll(users);
            log.info("✅ 테스트용 유저 1,000명 생성 완료!");
        }

        // 2. 공연 및 좌석 생성
        if (concertRepository.count() == 0) {
            Concert concert = new Concert("아이유 콘서트 2026");
            concertRepository.save(concert);

            ConcertOption option = new ConcertOption(concert, LocalDateTime.now().plusDays(30));
            concertOptionRepository.save(option);

            // 좌석 50개 생성
            List<Seat> seats = new ArrayList<>();
            for (int i = 1; i <= 50; i++) {
                Seat seat = new Seat(option, i, Seat.SeatStatus.AVAILABLE);
                seats.add(seat);
            }
            seatRepository.saveAll(seats);
            System.out.println("✅ 테스트용 좌석 50개 생성 완료!");
        }
    }
}
