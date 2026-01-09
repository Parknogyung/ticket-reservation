package com.server.portfolio.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password; // BCrypt 암호화 저장

    private Long point;

    @Enumerated(EnumType.STRING)
    private Role role; // USER, ADMIN

    public enum Role {
        USER, ADMIN
    }

    // 포인트 차감 로직 (도메인 메서드)
    public void usePoint(Long amount) {
        if (this.point < amount) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
        this.point -= amount;
    }
}
