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

    private String externalStadiumId;   // 외부API 를 통해 전달받은 경기장 ID
    private String name;
    private String sportId;
    private String city;

    public static Stadium of(String externalStadiumId, String name, String sportId, String city) {
        return Stadium.builder()
                .externalStadiumId(externalStadiumId)
                .name(name)
                .sportId(sportId)
                .city(city)
                .build();
    }
}
