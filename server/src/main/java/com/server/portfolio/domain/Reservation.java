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
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 주문 번호(Reservation ID)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ManyToOne: 한 좌석에 대해 '취소된 예약' 내역이 여러 개 있을 수 있으므로
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(nullable = false)
    private LocalDateTime reservationTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    public enum ReservationStatus {
        PENDING,   // 예약 요청 중 (임시 점유)
        SUCCESS,   // 결제 완료 (최종 확정)
        CANCELLED  // 취소됨 (시간 초과 등)
    }

    @Builder
    public Reservation(User user, Seat seat, LocalDateTime reservationTime, ReservationStatus status) {
        this.user = user;
        this.seat = seat;
        this.reservationTime = reservationTime;
        this.status = status;
    }

    // 비즈니스 로직 (상태 변경)
    public void confirm() {
        this.status = ReservationStatus.SUCCESS;
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }
}
