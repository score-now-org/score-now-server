package com.scorenow.scorenow_api.domain.stadium.entity;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stadiums")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
@ToString
public class Stadium extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sportId;

    private String name;
    private String city;

    @Enumerated(EnumType.STRING)
    private DataOrigin dataOrigin;    // 데이터 원천 (외부 API, 수동등록)

    public static Stadium of(String name, Long sportId, String city) {
        return of(name, sportId, city, null);
    }

    public static Stadium of(String name, Long sportId, String city, DataOrigin dataOrigin) {
        return Stadium.builder()
                .name(name)
                .sportId(sportId)
                .city(city)
                .dataOrigin(dataOrigin)
                .build();
    }
}
