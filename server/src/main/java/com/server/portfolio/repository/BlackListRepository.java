package com.server.portfolio.repository;

import com.server.portfolio.domain.BlackList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlackListRepository extends JpaRepository<BlackList, Long> {
    boolean existsByIpAddress(String ipAddress);
}
