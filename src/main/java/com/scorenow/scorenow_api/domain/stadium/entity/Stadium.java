package com.scorenow.scorenow_api.domain.stadium.entity;

import com.scorenow.scorenow_api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stadiums")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class Stadium extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sportId;

    private String name;
    private String city;

    public static Stadium of(String name, Long sportId, String city) {
        return Stadium.builder()
                .name(name)
                .sportId(sportId)
                .city(city)
                .build();
    }
}
