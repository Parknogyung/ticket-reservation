package com.server.portfolio.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // [관계 설정] 좌석은 '하나의 공연 회차'에 속함
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_option_id", nullable = false)
    private ConcertOption concertOption;

    @Column(nullable = false)
    private Integer seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    // 낙관적 락을 위한 버전
    @Version
    private Long version;

    public enum SeatStatus {
        AVAILABLE, RESERVED, SOLD
    }

    @Builder
    public Seat(ConcertOption concertOption, Integer seatNumber, SeatStatus status) {
        this.concertOption = concertOption;
        this.seatNumber = seatNumber;
        this.status = status;
    }

    public void reserve() {
        if (this.status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException("이미 예약된 좌석입니다.");
        }
        this.status = SeatStatus.RESERVED;
    }
}
