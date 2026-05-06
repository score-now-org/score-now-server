package com.scorenow.scorenow_api.domain.stadium.entity;

import com.scorenow.scorenow_api.external.common.ExternalProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stadium_external_mappings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_external_stadium_id_sport_id",   // TODO: 유니크키 제약조건 이름 변경
                        columnNames = {"provider", "external_sport_id", "external_stadium_id", "internal_stadium_id"})})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class StadiumExternalMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false)
    private ExternalProvider provider;

    @Column(name = "external_sport_id", nullable = false)
    private String externalSportId;     // 외부API 를 통해 전달받은 종목 ID

    @Column(name = "external_stadium_id", nullable = false)
    private String externalStadiumId;   // 외부API 를 통해 전달받은 경기장 ID

    @Column(name = "internal_stadium_id", nullable = false)
    private Long internalStadiumId;     // 시스템 내부에서 채번한 경기장 ID

    public static StadiumExternalMapping of(ExternalProvider provider, String externalSportId, String externalStadiumId, Long internalStadiumId) {
        return new StadiumExternalMapping(null, provider, externalSportId, externalStadiumId, internalStadiumId);
    }

}
