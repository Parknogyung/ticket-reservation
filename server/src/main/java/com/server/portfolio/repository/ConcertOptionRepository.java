package com.server.portfolio.repository;

import com.server.portfolio.domain.ConcertOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertOptionRepository extends JpaRepository<ConcertOption, Long> {
}
