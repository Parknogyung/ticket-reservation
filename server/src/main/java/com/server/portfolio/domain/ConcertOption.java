package com.server.portfolio.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "concert_options") // 테이블 명시
public class ConcertOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // [성능 Tip] 즉시 로딩(EAGER)은 N+1 문제를 유발하므로 지연 로딩(LAZY) 필수
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @Column(nullable = false)
    private LocalDateTime concertDate; // 공연 일시

    @Column(nullable = false)
    private Long price;

    @Builder
    public ConcertOption(Concert concert, LocalDateTime concertDate, Long price) {
        this.concert = concert;
        this.concertDate = concertDate;
        this.price = price != null ? price : 0L;
    }
}
