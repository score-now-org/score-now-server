package com.scorenow.scorenow_api.domain.stadium.entity;

import com.scorenow.scorenow_api.external.common.ApiProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stadium_external_mappings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stadium_external_mappings",
                        columnNames = {"provider", "api_sport_id", "api_stadium_id", "internal_stadium_id"})})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class StadiumExternalMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private ApiProvider provider;

    @Column(name = "api_sport_id", nullable = false)
    private String apiSportId;     // 외부API 를 통해 전달받은 종목 ID

    @Column(name = "api_stadium_id", nullable = false)
    private String apiStadiumId;   // 외부API 를 통해 전달받은 경기장 ID

    @Column(name = "internal_stadium_id", nullable = false)
    private Long internalStadiumId;     // 시스템 내부에서 채번한 경기장 ID

    public static StadiumExternalMapping of(ApiProvider provider, String apiSportId, String apiStadiumId, Long internalStadiumId) {
        return new StadiumExternalMapping(null, provider, apiSportId, apiStadiumId, internalStadiumId);
    }

}
