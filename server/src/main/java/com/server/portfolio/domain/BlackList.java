package com.server.portfolio.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlackList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ipAddress;
    private String reason; // 차단 사유 (예: "1초 내 50회 접속 시도")
    private LocalDateTime blockedAt;

    public BlackList(String ipAddress, String reason) {
        this.ipAddress = ipAddress;
        this.reason = reason;
        this.blockedAt = LocalDateTime.now();
    }
}