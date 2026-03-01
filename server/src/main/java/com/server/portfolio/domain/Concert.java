package com.server.portfolio.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "concerts")
public class Concert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column
    private String venue; // 공연 지역

    @Column(length = 1000)
    private String imageUrl; // 포스터 이미지 URL

    @Builder
    public Concert(String title, String venue, String imageUrl) {
        this.title = title;
        this.venue = venue;
        this.imageUrl = imageUrl;
    }
}