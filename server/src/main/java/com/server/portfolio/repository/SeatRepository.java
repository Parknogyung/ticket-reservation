package com.server.portfolio.repository;

import com.ticket.portfolio.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // 특정 공연 회차의 '예약 가능(AVAILABLE)'한 좌석만 조회
    // JPQL을 사용하여 객체지향적으로 쿼리 작성
    @Query("SELECT s FROM Seat s WHERE s.concertOption.id = :concertOptionId AND s.status = 'AVAILABLE'")
    List<Seat> findAvailableSeats(@Param("concertOptionId") Long concertOptionId);

    // 좌석 예약을 위해 조회할 때 락(Lock)을 걸 수도 있음 (선택 사항)
    // 여기서는 낙관적 락(@Version)을 쓰므로 별도의 비관적 락(@Lock)은 일단 생략합니다.
}