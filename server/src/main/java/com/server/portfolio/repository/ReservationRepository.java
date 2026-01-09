package com.server.portfolio.repository;

import com.server.portfolio.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    // 특정 유저의 예약 내역 조회
    List<Reservation> findByUserId(Long userId);
}