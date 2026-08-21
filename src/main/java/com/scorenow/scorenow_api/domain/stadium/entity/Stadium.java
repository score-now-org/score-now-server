package com.scorenow.scorenow_api.domain.stadium.entity;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.util.Objects;

import static com.scorenow.scorenow_api.domain.common.enums.DataOrigin.MANUAL;

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
    @Column(name = "data_origin", nullable = false, updatable = false)
    private DataOrigin dataOrigin;    // 데이터 원천 (외부 API, 수동등록)

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    @ColumnDefault("true")
    private boolean isActive = true;

    @Deprecated
    public static Stadium of(String name, Long sportId, String city) {
        return of(name, sportId, city, MANUAL);
    }

    public static Stadium of(String name, Long sportId, String city, DataOrigin dataOrigin) {
        return Stadium.builder()
                .name(name)
                .sportId(sportId)
                .city(city)
                .dataOrigin(Objects.requireNonNull(dataOrigin, "경기장 데이터 원천은 필수입니다."))
                .build();
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateCity(String city) {
        this.city = city;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
