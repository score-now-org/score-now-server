package com.scorenow.scorenow_api.domain.sport.entity;

import com.scorenow.scorenow_api.external.common.ApiProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sport_external_mappings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sport_external_mappings",
                        columnNames = {"provider", "api_sport_id"})})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class SportExternalMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private ApiProvider provider;

    @Column(name = "api_sport_id", nullable = false)
    private String apiSportId;     // 외부API 를 통해 전달받은 종목 ID

    @Column(name = "internal_sport_id", nullable = false)
    private Long internalSportId;     // 시스템 내부에서 채번한 경기장 ID

    public static SportExternalMapping of(ApiProvider provider, String apiSportId, Long internalSportId) {
        return new SportExternalMapping(null, provider, apiSportId, internalSportId);
    }
}
